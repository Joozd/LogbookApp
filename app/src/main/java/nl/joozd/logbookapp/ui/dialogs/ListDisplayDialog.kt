package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogListDisplayBinding
import nl.joozd.logbookapp.databinding.ItemPickerDialogBinding
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class ListDisplayDialog: JoozdlogFragment() {
    /**
     * Instantiate the fragment, apply [title] and [valuesToDisplay].
     * They will be saved for rotation and recreation.
     */
    var title: String = ""
    var valuesToDisplay: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogListDisplayBinding.bind(inflater.inflate(R.layout.dialog_list_display, container, false)).apply{
            savedInstanceState?.getString(TITLE)?.let { title = it }
            savedInstanceState?.getStringArrayList(CONTENTS)?.let { valuesToDisplay = it }

            titleDialogListDisplay.text = title

            recyclerviewDialogListDisplay.layoutManager = LinearLayoutManager(context)
            recyclerviewDialogListDisplay.adapter = SimpleAdapter(valuesToDisplay)

            headerLayout.setOnClickListener {  } // do nothing
            bodyLayout.setOnClickListener {  } // do nothing
            listDisplayDialogBackground.setOnClickListener { } // do nothing

            closeDialogListDisplay.setOnClickListener {
                closeFragment()
            }
        }.root

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(TITLE, title)
        outState.putStringArrayList(CONTENTS, ArrayList(valuesToDisplay))
    }

    private class SimpleAdapter(private val strings: List<String>): RecyclerView.Adapter<SimpleAdapter.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_picker_dialog, parent, false))

        override fun getItemCount(): Int = strings.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.text = strings[position]
        }

        class ViewHolder(containerView: View) :
            RecyclerView.ViewHolder(containerView) {
            val binding = ItemPickerDialogBinding.bind(containerView)

            var text: String
                get() = binding.nameTextView.text.toString()
                set(it) { binding.nameTextView.text = it }
        }
    }

    companion object{
        private const val TITLE = "TITLE"
        private const val CONTENTS  = "CONTENTS"
    }
}