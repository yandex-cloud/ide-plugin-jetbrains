package yandex.cloud.toolkit.api.service

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import yandex.cloud.toolkit.api.auth.CloudAuthData
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperation
import yandex.cloud.toolkit.api.resource.impl.model.CloudOperationResult

interface CloudOperationTracker {

    fun awaitOperationEnd(
        project: Project,
        authData: CloudAuthData,
        operation: CloudOperation,
    ): CloudOperationResult

    fun awaitOperationsEnd(
        project: Project,
        authData: CloudAuthData,
        operations: List<CloudOperation>
    ): List<CloudOperationResult>

    companion object {
        val instance: CloudOperationTracker get() = ServiceManager.getService(CloudOperationTracker::class.java)
    }
}

@Throws(Exception::class)
fun CloudOperation.awaitEnd(project: Project, authData: CloudAuthData): CloudOperationResult =
    CloudOperationTracker.instance.awaitOperationEnd(project, authData, this)

@Throws(Exception::class)
fun List<CloudOperation>.awaitEnd(project: Project, authData: CloudAuthData): List<CloudOperationResult> =
    CloudOperationTracker.instance.awaitOperationsEnd(project, authData, this)