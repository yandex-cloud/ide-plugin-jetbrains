package yandex.cloud.toolkit.util.listener

import com.intellij.openapi.ui.Splitter
import yandex.cloud.toolkit.ui.component.SpoilerListener
import yandex.cloud.toolkit.ui.component.SpoilerPanel

class SplitterSpoilerConnectivity(
    val splitter: Splitter,
    val spoiler: SpoilerPanel<*>,
    val openedProportion: Float = 0.5F
) : SpoilerListener, SplitterProportionListener {

    override fun onOpened() {
        if (splitter.proportion == 1.0f) splitter.proportion = openedProportion
    }

    override fun onClosed() {
        splitter.proportion = 1.0f
    }

    override fun proportionChanged(proportion: Float) {
        val opened = proportion < 1.0f
        if (opened != spoiler.isOpened) spoiler.toggle()
    }

    fun install() {
        splitter.proportion = if (spoiler.isOpened) openedProportion else 1.0f

        installOn(splitter)
        spoiler.addSpoilerListener(this)
    }
}