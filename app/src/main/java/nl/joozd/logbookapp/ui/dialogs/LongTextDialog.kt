package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogLongTextBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

/*
 * Use this for dialogs that just show a long text. Long text gets into a
 */
abstract class LongTextDialog: JoozdlogFragment() {
    // title of the dialog
    abstract val titleRes: Int

    /**
     * Text for the dialog. Can be a flow if it changes or is loaded async.
     * If both [textFlow] and [text] are provided, initially [text] will be shown and it will be replaced by [textFlow] when it is loaded.
     * If neither is provided, no text will be shown.
     */
    open val textFlow: Flow<CharSequence>? = null
    open val text: CharSequence? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogLongTextBinding.bind(inflater.inflate(R.layout.dialog_long_text, container, false)).apply{
            dialogLongTextTitleTextView.setText(titleRes)

            dialogLongTextTextView.movementMethod = LinkMovementMethod.getInstance()

            text?.let{
                dialogLongTextTextView.text = it
            }

            textFlow?.launchCollectWhileLifecycleStateStarted{
                dialogLongTextTextView.text = it
            }

            headerLayout.setOnClickListener {  } // do nothing
            bodyLayout.setOnClickListener {  } // do nothing
            dialogLongTextBackground.setOnClickListener { } // do nothing

            okButton.setOnClickListener {
                closeFragment()
            }
        }.root

    protected fun createFlowFromRaw(res: Int) = flow{
        emit(resources.openRawResource(res)
            .reader()
            .readText()
        )
    }
}