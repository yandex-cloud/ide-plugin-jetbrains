package yandex.cloud.sdk

import yandex.cloud.api.logs.v1.LogEventServiceGrpc

object LocalGrpcMapping {

    fun register() {
        StubToServiceMapping.map.apply {
            put(LogEventServiceGrpc.LogEventServiceStub::class.java, "log-event")
            put(LogEventServiceGrpc.LogEventServiceBlockingStub::class.java, "log-event")
            put(LogEventServiceGrpc.LogEventServiceFutureStub::class.java, "log-event")
        }

        ServiceToEndpointMapping.map.apply {
            put("log-event", "logs.api.cloud.yandex.net:443");
        }
    }
}