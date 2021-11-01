package yandex.cloud.toolkit.util.remote.resource

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.awt.Color

interface PresentableResourceStatus {

    fun display(presentation: PresentationData)
    fun display(component: SimpleColoredComponent)

    object Loading : SimpleResourceStatus(
        "Loading...", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
    )

    object FailedToLoad : SimpleResourceStatus(
        "Failed to load", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    object Unauthenticated : SimpleResourceStatus(
        "Unauthenticated", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    object ProfileCorrupted : SimpleResourceStatus(
        "Profile Corrupted", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    object OAuthFailed : SimpleResourceStatus(
        "OAuth Failed", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    object Creating : SimpleResourceStatus(
        "Creating...", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
    )

    object Canceled : SimpleResourceStatus(
        "Canceled", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, WARNING_COLOR)
    )

    object Resolving : SimpleResourceStatus(
        "Resolving...", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
    )

    object Unresolved : SimpleResourceStatus(
        "Unresolved", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    object NotFound : SimpleResourceStatus(
        "Not Found", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, WARNING_COLOR)
    )

    object ConcurrentAction : SimpleResourceStatus(
        "Concurrent Action", SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
    )

    companion object {
        private val WARNING_COLOR = JBColor(Color.orange, Color(138, 138, 0))

        fun byStatusRE(e: StatusRuntimeException): PresentableResourceStatus {
            if (e.status.code == Status.Code.UNAUTHENTICATED) return Unauthenticated

            val status = e.status.description ?: e.status.code.toString()
            return SimpleResourceStatus(
                status, SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.RED)
            )
        }

        fun tags(tags: List<String>?): PresentableResourceStatus? = when {
            tags.isNullOrEmpty() -> null
            else -> CloudResourceStatusTags(tags)
        }
    }
}

open class SimpleResourceStatus(val displayName: String, private val displayAttributes: SimpleTextAttributes?) :
    PresentableResourceStatus {

    override fun display(presentation: PresentationData) {
        presentation.addText(displayName, displayAttributes)
    }

    override fun display(component: SimpleColoredComponent) {
        when (displayAttributes) {
            null -> component.append(displayName)
            else -> component.append(displayName, displayAttributes)
        }
    }

    override fun toString(): String = displayName
}

class MergedResourceStatus(val left: PresentableResourceStatus, val right: PresentableResourceStatus) :
    PresentableResourceStatus {
    override fun display(presentation: PresentationData) {
        left.display(presentation)
        presentation.addText(" ", SimpleTextAttributes.GRAY_ATTRIBUTES)
        right.display(presentation)
    }

    override fun display(component: SimpleColoredComponent) {
        left.display(component)
        component.append(" ", SimpleTextAttributes.GRAY_ATTRIBUTES)
        right.display(component)
    }

    override fun toString(): String = "$left $right"
}

class CloudResourceStatusTags(tags: List<String>) : SimpleResourceStatus(
    tags.joinToString(" "),
    SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.GRAY)
)

class CloudResourceStatusZone(zoneId: String) : SimpleResourceStatus(
    zoneId,
    SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER, JBColor.GRAY)
)

operator fun PresentableResourceStatus?.plus(other: PresentableResourceStatus?): PresentableResourceStatus? = when {
    this == null -> other
    other == null -> this
    else -> MergedResourceStatus(this, other)
}

fun RemoteResource<*>?.asPresentableStatus(): PresentableResourceStatus? = when (val state = this?.state) {
    is LoadingResourceState -> PresentableResourceStatus.Loading
    is FailedResourceState -> state.error.status
    else -> null
}