package yandex.cloud.toolkit.listener

import com.intellij.ide.AppLifecycleListener
import io.grpc.LoadBalancerRegistry
import io.grpc.util.OutlierDetectionLoadBalancerProvider

class StartupListener : AppLifecycleListener {
    override fun appStarted() {
        LoadBalancerRegistry.getDefaultRegistry().register(OutlierDetectionLoadBalancerProvider())
    }
}