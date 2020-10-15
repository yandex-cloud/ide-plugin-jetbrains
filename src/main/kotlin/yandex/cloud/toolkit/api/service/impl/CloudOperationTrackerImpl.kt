package yandex.cloud.toolkit.api.service.impl

import com.intellij.openapi.project.Project
import yandex.cloud.api.operation.OperationOuterClass
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperation
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperationResult
import yandex.cloud.toolkit.api.service.CloudOperationTracker
import yandex.cloud.toolkit.api.service.CloudRepository
import yandex.cloud.toolkit.ui.action.ShowOperationLogsAction
import yandex.cloud.toolkit.util.*
import java.time.Duration
import java.util.concurrent.TimeoutException

class CloudOperationTrackerImpl : CloudOperationTracker {

    companion object {
        private val DELAY = Duration.ofSeconds(1).toMillis()
        private val TIMEOUT = Duration.ofMinutes(5).toMillis()

        private val LOG_PATTERN = "^.*, logs: (.*)$".toRegex()
    }

    override fun awaitOperationEnd(
        project: Project,
        authData: CloudAuthData,
        operation: CloudOperation,
    ): CloudOperationResult = awaitOperationsEnd(project, authData, listOf(operation)).first()

    override fun awaitOperationsEnd(
        project: Project,
        authData: CloudAuthData,
        operations: List<CloudOperation>
    ): List<CloudOperationResult> {
        val currentOperations: Array<Maybe<OperationOuterClass.Operation>> = operations.map { it.data }.toTypedArray()
        val statuses = BooleanArray(operations.size) { operations[it].data.getOrNull()?.done ?: true }
        var activeCount = statuses.count { !it }

        var passedTime = 0L

        while (activeCount > 0 && passedTime < TIMEOUT) {
            Thread.sleep(DELAY)
            passedTime += DELAY

            for (i in operations.indices) {
                if (statuses[i]) continue

                val newOperation = currentOperations[i].map {
                    CloudRepository.instance.getOperation(authData, it.id)
                }

                currentOperations[i] = newOperation
                if (newOperation.value?.done != false) {
                    activeCount--
                    statuses[i] = true
                }
            }
        }

        return Array(operations.size) {
            var logsUrl: String? = null
            val result = currentOperations[it].tryMap { operation ->
                val logs = operation.extractLogsUrl()
                logsUrl = logs

                when {
                    operation.hasResponse() -> when (logs) {
                        null -> EmptyActionsBundle
                        else -> StaticActionsBundle(ShowOperationLogsAction(operation.id, logs))
                    }

                    operation.hasError() -> {
                        var error: Exception = RuntimeException(operation.error.message)
                        if (logs != null) error += ShowOperationLogsAction(operation.id, logs)
                        throw error
                    }

                    else -> throw TimeoutException("Operation timed out")
                }
            }

            val newOperation = CloudOperation(operations[it].name, currentOperations[it])
            CloudOperationResult(newOperation, result, logsUrl)
        }.toList()
    }

    private fun OperationOuterClass.Operation.extractLogsUrl(): String? {
        val groups = LOG_PATTERN.find(description)?.groups
        return if (groups != null && groups.size > 1) groups[1]?.value else null
    }
}