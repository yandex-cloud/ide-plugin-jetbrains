package yandex.cloud.toolkit.util

import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

fun interface Restriction<in V> {
    fun test(value: V): String?
}

class Restrictions<V>(val name: String) : Restriction<V> {

    private val restrictions = mutableListOf<Restriction<V>>()

    fun validate(value: V, component: JComponent? = null): ValidationInfo? {
        val errorMessage = test(value) ?: return null
        return ValidationInfo("$name: $errorMessage", component)
    }

    fun addRestriction(restriction: Restriction<V>) {
        restrictions.add(restriction)
    }

    override fun test(value: V): String? {
        for (restriction in restrictions) {
            return restriction.test(value) ?: continue
        }
        return null
    }
}

fun <V> restrictions(name: String, block: Restrictions<V>.() -> Unit): Restrictions<V> =
    Restrictions<V>(name).apply(block)

inline fun <V> Restrictions<V>.require(crossinline condition: (V) -> Boolean, crossinline message: () -> String) {
    addRestriction { if (!condition(it)) message() else null }
}

inline fun <V> Restrictions<V>.requireNot(crossinline condition: (V) -> Boolean, crossinline message: () -> String) {
    addRestriction { if (condition(it)) message() else null }
}


fun <V> Restrictions<V>.check(restriction: Restriction<V>) {
    addRestriction(restriction)
}

fun Restrictions<String>.textMinLength(length: Int) {
    addRestriction {
        if (it.length < length) "min length is $length" else null
    }
}

fun Restrictions<String>.textMaxLength(length: Int) {
    addRestriction {
        if (it.length > length) "max length is $length" else null
    }
}

fun Restrictions<String>.textLength(range: IntRange) {
    textLength(range.first, range.last)
}

fun Restrictions<String>.textLength(min: Int, max: Int) {
    addRestriction {
        if (it.length !in min..max) "length limit is $min..$max" else null
    }
}

fun Restrictions<String>.textIsNotEmpty() {
    addRestriction {
        if (it.isEmpty() || it.isBlank()) "can not be empty" else null
    }
}

fun Restrictions<String>.textPattern(regex: Regex) {
    addRestriction {
        if (!regex.matches(it)) "invalid format" else null
    }
}

fun Restrictions<String>.textPattern(regex: String) {
    textPattern(regex.toRegex())
}
