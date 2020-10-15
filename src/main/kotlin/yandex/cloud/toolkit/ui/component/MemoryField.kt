package yandex.cloud.toolkit.ui.component

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import yandex.cloud.toolkit.util.*
import java.awt.GridBagLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JSlider

class MemoryField(val minMemory: Int, val maxMemory: Int, val memoryStep: Int, val defaultMemory: Int, val keyMemoryValues: IntArray) : YCPanel(GridBagLayout()) {

    private val slider = JSlider(
        JSlider.HORIZONTAL,
        minMemory / memoryStep, maxMemory / memoryStep, defaultMemory / memoryStep
    )

    private val indicator = JBTextField("").apply { isEditable = false }

    var valueBytes: Long
        get() = (slider.value.toLong() * memoryStep).coerceIn(minMemory.toLong(), maxMemory.toLong()) * 1024 * 1024
        set(value) {
            slider.value = (value / memoryStep / 1024 / 1024).toInt()
        }

    init {
        updateIndicator()
        slider.addChangeListener { updateIndicator() }
        slider.paintTicks = true
        slider.paintLabels = true
        slider.minorTickSpacing = 1
        slider.labelTable = Hashtable<Int, JComponent>().apply {
            keyMemoryValues.forEach {
                this[it / memoryStep] = JBLabel(it.toString())
            }
        }

        YCUI.gridBag(horizontal = true) {
            indicator.withPreferredWidth(90) addAs nextln(0.0)
            slider addAs next(1.0)
        }
    }

    private fun updateIndicator() {
        indicator.text = "${slider.value * memoryStep} MB"
    }
}