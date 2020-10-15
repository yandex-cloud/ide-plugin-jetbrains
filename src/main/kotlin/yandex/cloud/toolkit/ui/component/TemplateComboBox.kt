package yandex.cloud.toolkit.ui.component

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.EditorTextField
import com.intellij.ui.SimpleTextAttributes
import yandex.cloud.toolkit.ui.dialog.RunFunctionDialog
import yandex.cloud.toolkit.util.task.backgroundTask
import java.io.FileNotFoundException
import javax.swing.DefaultComboBoxModel
import javax.swing.JList

class TemplateComboBox(
    val project: Project,
    val targetEditor: EditorTextField,
    val templatePath: String,
    val templates: Array<Pair<String, String?>>
) : ComboBox<Pair<String, String?>>() {

    init {
        model = DefaultComboBoxModel(templates)
        renderer = ListRenderer()

        addActionListener {
            selectTemplate(templates[selectedIndex].second)
        }
    }

    private fun selectTemplate(path: String?) {
        fun setInputText(text: String) = runInEdt(ModalityState.stateForComponent(this)) { targetEditor.text = text }

        if (path == null) {
            setInputText("")
            return
        }

        backgroundTask(project, "Yandex.Cloud Functions") {
            text = "Loading template..."

            val templateText = tryDo {
                val resourceStream = RunFunctionDialog::class.java.classLoader.getResourceAsStream(
                    "templates/$templatePath/$path"
                ) ?: throw FileNotFoundException("Template file not found")

                resourceStream.bufferedReader().use {
                    it.readText()
                }
            } onFail {
                setInputText("Failed to load template: ${it.message}")
            }

            setInputText(templateText)
        }
    }

    private class ListRenderer : ColoredListCellRenderer<Pair<String, String?>>() {
        override fun customizeCellRenderer(
            list: JList<out Pair<String, String?>>,
            value: Pair<String, String?>,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            val color = when (value.second) {
                null -> SimpleTextAttributes.GRAY_ATTRIBUTES
                else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
            }
            append(value.first, color)
        }
    }
}