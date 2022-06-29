package icons

import com.intellij.openapi.util.IconLoader

object CloudIcons {

    object Icons {
        @JvmField
        val CloudExplorer = IconLoader.getIcon("/icons/cloud-explorer.svg", CloudIcons::class.java)
    }

    object Actions {
        val Delete = IconLoader.getIcon("/icons/delete.svg", CloudIcons::class.java)
    }

    object Nodes {
        val Label = IconLoader.getIcon("/icons/label.svg", CloudIcons::class.java)
        val Profile = IconLoader.getIcon("/icons/profile.svg", CloudIcons::class.java)
        val ScalingPolicy = IconLoader.getIcon("/icons/scaling-policy.svg", CloudIcons::class.java)
        val Variable = IconLoader.getIcon("/icons/variable.svg", CloudIcons::class.java)
    }

    object Status {
        val Success = IconLoader.getIcon("/icons/success.svg", CloudIcons::class.java)
    }

    object Other {
        val Terminal = IconLoader.getIcon("/icons/terminal.svg", CloudIcons::class.java)
    }

    object Resources {
        val CloudUser = IconLoader.getIcon("/icons/cloud-user.svg", CloudIcons::class.java)
        val Cloud = IconLoader.getIcon("/icons/cloud.svg", CloudIcons::class.java)
        val Function = IconLoader.getIcon("/icons/cloud-function.svg", CloudIcons::class.java)
        val ApiGateway = IconLoader.getIcon("/icons/cloud-gateway.svg", CloudIcons::class.java)
        val Trigger = IconLoader.getIcon("/icons/cloud-trigger.svg", CloudIcons::class.java)
        val ServiceAccount = IconLoader.getIcon("/icons/cloud-service-account.svg", CloudIcons::class.java)
        val Network = IconLoader.getIcon("/icons/vpc-network.svg", CloudIcons::class.java)
        val Subnet = IconLoader.getIcon("/icons/vpc-subnet.svg", CloudIcons::class.java)
        val Lockbox = IconLoader.getIcon("/icons/lockbox.svg", CloudIcons::class.java)
    }
}