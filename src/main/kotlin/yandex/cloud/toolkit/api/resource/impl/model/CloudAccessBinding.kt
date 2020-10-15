package yandex.cloud.toolkit.api.resource.impl.model

import yandex.cloud.api.access.Access

enum class AccessBindingType(val id: String) {
    USER_ACCOUNT("userAccount"),
    SERVICE_ACCOUNT("serviceAccount"),
    FEDERATED_USER("federatedUser"),
    SYSTEM("system")
}

fun Access.Subject.hasType(type: AccessBindingType): Boolean =
    this.type == type.id

fun Access.AccessBinding.hasSubject(serviceAccount: CloudServiceAccount): Boolean =
    subject.hasType(AccessBindingType.SERVICE_ACCOUNT) && subject.id == serviceAccount.id