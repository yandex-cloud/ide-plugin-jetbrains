package yandex.cloud.toolkit.api.resource.impl.model

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.SimpleNode
import icons.CloudIcons
import yandex.cloud.api.serverless.functions.v1.FunctionOuterClass
import yandex.cloud.toolkit.api.resource.*
import yandex.cloud.toolkit.api.resource.impl.*
import yandex.cloud.toolkit.ui.action.*
import yandex.cloud.toolkit.ui.explorer.CloudExplorerContext
import yandex.cloud.toolkit.ui.node.DummySimpleNode
import yandex.cloud.toolkit.util.remote.resource.PresentableResourceStatus
import java.util.*

class CloudFunctionGroup(val folder: CloudFolder) :
    SimpleHierarchicalCloudResource<CloudFunctionGroup, CloudFunction>(), FolderGroup {
    override val id: String get() = "${folder.id}-cloud-functions"
    override val name: String = "Cloud Functions"
    override val parent: HierarchicalCloudResource get() = folder
    override val descriptor: CloudResourceDescriptor get() = Descriptor
    override val isTrueCloudResource: Boolean get() = false

    override val mainDependency get() = Functions
    override val mainDependencyName: String get() = descriptor.type
    override val mainDependencyLoader get() = CloudFunctionsLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Functions")

    val functions: List<CloudFunction>? get() = Functions.of(this).get()

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += CreateOrEditFunctionAction(folder, null)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = folder.getGroupPath(this, innerPath)
    override fun getLocation(joiner: StringJoiner) = folder.getLocation(joiner)

    object Functions : CloudDependency<CloudFunctionGroup, List<CloudFunction>>()
    object Function : CloudDependency<CloudFunctionGroup, CloudFunction>()

    object Descriptor : CloudResourceDescriptor(
        "functions",
        "Cloud Functions",
        icon = null,
        isPinnable = true
    )
}

class CloudFunction(
    val data: FunctionOuterClass.Function,
    val group: CloudFunctionGroup
) : SimpleHierarchicalCloudResource<CloudFunction, CloudFunctionVersion>() {

    override val id: String get() = data.id
    override val name: String get() = data.name
    override val parent: HierarchicalCloudResource get() = group
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    override val mainDependency get() = FunctionVersions
    override val mainDependencyName: String get() = "versions"
    override val mainDependencyLoader get() = CloudFunctionVersionsLoader

    override val emptyNode: SimpleNode get() = DummySimpleNode("No Versions")

    val fullName: String get() = if (group.folder.isVirtual) name else "${group.folder.fullName}/$name"

    val versions: List<CloudFunctionVersion>? get() = FunctionVersions.of(this).get()

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        actions += DeployFunctionAction(context.project, this)
        actions += RunFunctionAction(this, null)
        actions += CreateOrEditFunctionAction(group.folder, this)
        actions += ShowFunctionLogsAction(this, null)
        actions += TrackFunctionLogsAction(this, null)
        actions += EditFunctionScalingPoliciesAction(this)
        actions += DeleteFunctionAction(this)
//        actions += GenerateFunctionRequestAction(explorer.project, this, null)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = group.getChildPath(this, innerPath)

    fun updateScalingPolicies(project: Project) {
        loadDependency(project, ScalingPolicies, CloudFunctionScalingPoliciesLoader)
    }

    companion object {

        fun forUser(user: CloudUser, functionId: String): SelfReference<CloudFunction> =
            user.resourceReference(functionId, SelfDependency)
    }

    object SelfDependency : CloudDependency<CloudFunction, CloudFunction>()
    object FunctionVersions : CloudDependency<CloudFunction, List<CloudFunctionVersion>>()
    object ScalingPolicies : CloudDependency<CloudFunction, List<CloudFunctionScalingPolicy>>()

    object Descriptor : CloudResourceDescriptor(
        "function",
        "Cloud Function",
        icon = CloudIcons.Resources.Function,
        isPinnable = true
    )
}

class CloudFunctionVersion(
    val data: FunctionOuterClass.Version,
    val function: CloudFunction
) : HierarchicalCloudResource() {
    override val id: String get() = data.id
    override val name: String get() = "$id (${data.runtime})"
    override val parent: HierarchicalCloudResource get() = function
    override val descriptor: CloudResourceDescriptor get() = Descriptor

    val fullName: String get() = "${function.fullName}/$id"

    override val status: PresentableResourceStatus?
        get() = when (data.status) {
            FunctionOuterClass.Version.Status.CREATING -> PresentableResourceStatus.Creating
            else -> PresentableResourceStatus.tags(data.tagsList)
        }

    override fun getExplorerPopupActions(context: CloudExplorerContext, actions: MutableList<AnAction>) {
        val tag = data.tagsList.firstOrNull()
        if (tag != null) {
            actions += RunFunctionAction(function, tag)
            actions += ShowFunctionLogsAction(function, this)
            actions += TrackFunctionLogsAction(function, this)
        }
        actions += RedeployFunctionVersionAction(context.project, function, id)
        actions += EditFunctionVersionTagsAction(this)
    }

    override fun getPath(innerPath: CloudResourcePath?): CloudResourcePath = function.getChildPath(this, innerPath)

    object Descriptor : CloudResourceDescriptor(
        "function-version",
        "Function Version",
        AllIcons.Nodes.Function
    )

    companion object {
        const val LATEST_TAG = "\$latest"
    }
}

data class CloudFunctionScalingPolicy(
    val tag: String,
    var zoneInstancesLimit: Long,
    var zoneRequestsLimit: Long
)

data class CloudFunctionSpec(
    val name: String,
    val description: String?
)

data class CloudFunctionUpdate(
    val name: String?,
    val description: String?
) {
    val isEmpty: Boolean get() = (name ?: description) == null
}

val List<CloudFunctionVersion>.latestVersion: CloudFunctionVersion?
    get() = findWithTag(CloudFunctionVersion.LATEST_TAG) ?: firstOrNull()

fun List<CloudFunctionVersion>.findWithTag(tag: String): CloudFunctionVersion? = find {
    it.data.tagsList?.contains(tag) == true
}