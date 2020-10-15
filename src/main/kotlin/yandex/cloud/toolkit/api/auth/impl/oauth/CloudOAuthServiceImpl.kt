package yandex.cloud.toolkit.api.auth.impl.oauth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.net.ssl.CertificateManager
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.apache.commons.httpclient.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.entity.ContentType
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.auth.CloudOAuthService
import yandex.cloud.toolkit.util.*
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import yandex.cloud.toolkit.util.task.modalTask
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CloudOAuthServiceImpl : CloudOAuthService {

    companion object {
        private val OM = ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)

        private const val OAUTH_APP_ID = "101d1dd21f0344ee9c4117819ca411d2"
        private const val OAUTH_SECRET = "e924810c788f43b096a3c1fb14498a1e"
        private const val OAUTH_URL = "https://oauth.yandex.ru"
        private const val LOGIN_INFO_URL = "https://login.yandex.ru/info"

        private const val OAUTH_TOKEN_FOR_CODE_URL = "$OAUTH_URL/token"
        private const val OAUTH_REQUEST_URL = "$OAUTH_URL/authorize?response_type=code&client_id=$OAUTH_APP_ID"

        private const val PORT = 3125

        private const val REDIRECT_PATH = "/redirect-to-auth"
        private const val REDIRECT_URL = "http://localhost:$PORT$REDIRECT_PATH"

        private val log = logger<CloudOAuthServiceImpl>()

        private const val TOKEN_REQUEST_TIMEOUT_SECONDS = 60

        private val TOKEN_KEY_ATTRIBUTE = CredentialAttributes("Yandex Cloud OAuth Secret")
        private const val TOKEN_MARKER = "YC.OAuth.Token"
    }

    private var httpClient: CloseableHttpClient

    private var awaitingToken = AtomicBoolean()

    private val tokenKey: Key

    init {
        val requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(100)
            .setConnectTimeout(1000)
            .setSocketTimeout(5000)
            .build()

        httpClient = HttpClients.custom()
            .setMaxConnTotal(2)
            .setConnectionTimeToLive(10, TimeUnit.SECONDS)
            .setDefaultRequestConfig(requestConfig)
            .setSSLContext(CertificateManager.getInstance().sslContext)
            .setUserAgent(CloudAuthData.USER_AGENT)
            .build()

        val rawTokenKey = PasswordSafe.instance.getPassword(TOKEN_KEY_ATTRIBUTE)
        if (rawTokenKey != null) {
            tokenKey = CryptUtils.readKey(rawTokenKey)
        } else {
            tokenKey = CryptUtils.generateKey()
            PasswordSafe.instance.setPassword(TOKEN_KEY_ATTRIBUTE, CryptUtils.writeKey(tokenKey))
        }
    }

    override fun encryptToken(token: String): String = doMaybe {
        CryptUtils.encrypt(tokenKey, "$TOKEN_MARKER$token")
    }.getOrNull() ?: ""

    override fun decryptToken(data: String): Maybe<String> {
        try {
            val decrypted = CryptUtils.decrypt(tokenKey, data)
            if (decrypted.startsWith(TOKEN_MARKER)) return just(decrypted.substring(TOKEN_MARKER.length))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return noResource("Illegal OAuth token")
    }

    override fun requestToken(project: Project, tokenCallback: (Maybe<String>) -> Unit) {
        if (awaitingToken.compareAndSet(false, true)) {
            modalTask(project, "OAuth", true) {
                text = "Waiting for OAuth token..."
                val token = requestToken(::isCanceled)
                tokenCallback(token)
                awaitingToken.set(false)
            }
        } else {
            tokenCallback(noResource("Other OAuth process is already running", PresentableResourceStatus.OAuthFailed))
        }
    }

    override fun requestLogin(token: String): Maybe<String> {
        val request = HttpGet(LOGIN_INFO_URL)
        request.setHeader("Authorization", "OAuth $token")

        return try {
            httpClient.execute(request).use { response ->
                when (response.statusLine.statusCode) {
                    200 -> {
                        val result: JsonNode = OM.readTree(response.entity.content)
                        if (result.has("login")) {
                            just(result["login"].textValue())
                        } else noResource("Login not found in response $result")
                    }
                    else -> noResource("Failed to get login, got http code " + response.statusLine.statusCode)
                }

            }
        } catch (e: IOException) {
            NoValue(e)
        }
    }

    private fun requestToken(isCanceled: () -> Boolean): Maybe<String> {
        val tokenCallback = CompletableFuture<String>()
        val urlOpenedCallback = CompletableFuture<Unit>()
        var httpServer: HttpServer? = null
        val notifier = OAuthWorkflowNotifier()

        return try {
            val authUri = URI(OAUTH_REQUEST_URL)
            httpServer = HttpServer.create(InetSocketAddress(PORT), 0)

            httpServer.createContext("/", handleCodeGetToken(tokenCallback))
            httpServer.createContext(REDIRECT_PATH, redirectTo(authUri, urlOpenedCallback))
            httpServer.executor = null
            httpServer.start()
            log.info("Server started")

            BrowserLauncher.instance.browse(URI(REDIRECT_URL))
            notifier.redirectStarted(authUri)
            urlOpenedCallback.thenRun(notifier::redirectPassed)

            var token: String?
            var timeout = TOKEN_REQUEST_TIMEOUT_SECONDS * 1000

            while (true) {
                if (isCanceled()) {
                    notifier.failure()
                    return noResource("OAuth process canceled", PresentableResourceStatus.Canceled)
                }
                if (timeout <= 0) {
                    notifier.failure()
                    return noToken("Token receive timeout")
                }

                token = tokenCallback.getNow(null)
                if (token != null) break

                Thread.sleep(250)
                timeout -= 250
            }

            notifier.tokenReceived()
            log.info("Got token")
            return just(token!!)
        } catch (e: IOException) {
            notifier.failure()
            log.warn("Failed to get OAuth token", e)
            noToken("IO Error: " + e.message)
        } catch (e: URISyntaxException) {
            notifier.failure()
            log.warn("Failed to get OAuth token", e)
            noToken(e.message ?: "")
        } catch (e: ExecutionException) {
            notifier.failure()
            log.warn("Failed to get OAuth token", e)
            noToken("Execution Error: " + e.message)
        } catch (e: InterruptedException) {
            notifier.failure()
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        } finally {
            notifier.close()
            httpServer?.stop(1)
        }
    }

    private fun noToken(message: String): Maybe<String> = noResource(message, PresentableResourceStatus.OAuthFailed)

    private fun redirectTo(redirUri: URI, completedFuture: CompletableFuture<Unit>) = HttpHandler { ex: HttpExchange ->
        try {
            ex.responseHeaders.add("Location", redirUri.toASCIIString())
            ex.sendResponseHeaders(HttpStatus.SC_MOVED_TEMPORARILY, 0)
            completedFuture.complete(Unit)
        } catch (e: java.lang.Exception) {
            completedFuture.completeExceptionally(e)
        } finally {
            ex.close()
        }
    }

    private fun handleCodeGetToken(tokenCallback: CompletableFuture<String>) = HttpHandler { ex: HttpExchange ->
        try {
            val uri = ex.requestURI
            if ("/" != uri.path) {
                ex.sendResponseHeaders(404, 0)
                return@HttpHandler
            }

            val paramsMap = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8).associate { it.name to it.value }

            when {
                "code" in paramsMap -> {
                    val code = paramsMap.getValue("code")
                    val token = getTokenFromCode(code)

                    tokenCallback.complete(token)
                    writeResult(ex, true)
                }

                "error" in paramsMap -> throwRE(
                    "OAuth failed with code",
                    paramsMap["error"],
                    paramsMap["error_description"],
                )

                else -> {
                    ex.sendResponseHeaders(400, 0)
                    log.warn("Failed to find any oauth query params in url $uri")
                }
            }
        } catch (e: Exception) {
            if (!tokenCallback.isDone) tokenCallback.completeExceptionally(e)
            writeResult(ex, false)
        } finally {
            ex.close()
        }
    }

    private fun writeResult(http: HttpExchange, success: Boolean) {
        val pageName = if (success) "oauth-success.html" else "oauth-fail.html"

        try {
            javaClass.classLoader.getResourceAsStream("html/$pageName").use { pageSrc ->
                http.responseHeaders.add("Content-Type", ContentType.TEXT_HTML.toString())
                http.sendResponseHeaders(200, 0)

                pageSrc?.copyTo(http.responseBody, 1024)
            }
        } catch (e: Exception) {
            log.warn("Failed to write html response", e)
        } finally {
            http.close()
        }
    }

    private fun getTokenFromCode(code: String): String? {
        val params = mutableListOf<NameValuePair>(
            BasicNameValuePair("grant_type", "authorization_code"),
            BasicNameValuePair("code", code),
            BasicNameValuePair("client_id", OAUTH_APP_ID),
            BasicNameValuePair("client_secret", OAUTH_SECRET)
        )

        val tokenRequest = HttpPost(OAUTH_TOKEN_FOR_CODE_URL)
        tokenRequest.entity = UrlEncodedFormEntity(params, StandardCharsets.UTF_8)

        return httpClient.execute(tokenRequest) { response ->
            val resultJson: JsonNode = OM.readTree(response.entity.content)
            when {
                resultJson.has("access_token") -> resultJson["access_token"].textValue()

                resultJson.has("error") -> throwRE(
                    "OAuth failed at token exchange with code",
                    resultJson["error"].textValue(),
                    resultJson["error_description"]
                )

                else -> throw RuntimeException("Unprocessable oauth response: $resultJson")
            }
        }
    }

    private fun throwRE(label: String, error: String?, description: Any?): Nothing {
        throw RuntimeException("$label $error: \"$description\"")
    }
}