package yandex.cloud.toolkit.util

import com.intellij.openapi.actionSystem.AnAction

interface ActionsBundle {

    fun getActions(actions: MutableList<AnAction>) {}
}

fun ActionsBundle.getActions(): MutableList<AnAction> {
    return mutableListOf<AnAction>().also { this.getActions(it) }
}

class StaticActionsBundle(private val actions: List<AnAction>) : ActionsBundle {

    constructor(action: AnAction) : this(listOf(action))

    override fun getActions(actions: MutableList<AnAction>) {
        actions += this.actions
    }
}

object EmptyActionsBundle : ActionsBundle {
    override fun getActions(actions: MutableList<AnAction>) {}
}

class RuntimeExceptionWithActions(message: String?, cause: Throwable) : RuntimeException(message, cause),
    ActionsBundle {
    private val actions = mutableListOf<AnAction>()

    override fun getActions(actions: MutableList<AnAction>) {
        actions += this.actions
    }

    operator fun plus(action: AnAction): RuntimeExceptionWithActions {
        this.actions += action
        return this
    }
}

operator fun Throwable.plus(action: AnAction): RuntimeExceptionWithActions {
    return RuntimeExceptionWithActions(message, this) + action
}

