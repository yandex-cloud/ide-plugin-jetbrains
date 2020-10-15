package yandex.cloud.toolkit.ui.component

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.components.JBCheckBox

class PropertyCheckBox(text: String, key: String, selected: Boolean) : JBCheckBox(text, selected) {

    private val defaultSelected = selected
    private val propertyKey = "YCUI.property_check_box.$key"

    init {
        loadProperty()

        model.addItemListener {
            saveProperty()
        }
    }

    fun loadProperty() {
        this.isSelected = PropertiesComponent.getInstance().getBoolean(propertyKey, defaultSelected)
    }

    fun saveProperty() {
        PropertiesComponent.getInstance().setValue(propertyKey, this.isSelected, defaultSelected)
    }
}