package nl.joozd.logbookapp.ui.dialogs

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.R


class UpdateMessageDialog: LongTextDialog() {
    override val titleRes = R.string.whats_new
    override val textFlow = createFlowFromRaw(R.raw.whats_new).map{
        HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}