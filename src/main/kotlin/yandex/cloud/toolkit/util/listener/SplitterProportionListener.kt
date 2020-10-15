package yandex.cloud.toolkit.util.listener

import com.intellij.openapi.ui.Splitter
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

fun interface SplitterProportionListener : PropertyChangeListener {

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (Splitter.PROP_PROPORTION == evt.propertyName) {
            proportionChanged(evt.newValue as Float)
        }
    }

    fun proportionChanged(proportion: Float)

    fun installOn(splitter: Splitter) = splitter.addPropertyChangeListener(this)
}

fun Splitter.addProportionListener(block: (Float) -> Unit) = SplitterProportionListener(block).installOn(this)