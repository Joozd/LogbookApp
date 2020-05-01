package nl.joozd.logbookapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_picker_dialog.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * Adapter for RecyclerView that can highlight one entry
 * Needs
 * @param color: the color of [itemBackground] will be set to this value for [selectedEntry]
 * @param itemLayout: Layout to be used for an item. MUST have a view named [itemBackground]
 * @param itemClick: Action to be performed onClick on an item
 * itemLayout must have a field named [itemBackground]
 */
class SelectableStringAdapter(
    var list: List<String> = emptyList(),
    var color: Int? = null,
    val itemLayout: Int = R.layout.item_picker_dialog,
    private val itemClick: (String) -> Unit
): RecyclerView.Adapter<SelectableStringAdapter.ViewHolder>() {
    private var selectedEntry: String? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(list[position])
        holder.itemBackground.setOnClickListener {
            itemClick(holder.nameTextView.text.toString())
        }
        holder.itemBackground.setBackgroundColor(if (holder.nameTextView.text.toString() == selectedEntry) color?: holder.itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else holder.itemBackground.ctx.getColor(R.color.none) )
    }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    fun updateList(l: List<String>){
        list = l
        notifyDataSetChanged()
    }

    fun selectActiveItem(activeItem: String) {
        if (activeItem in list) {
            val previousIndex = list.indexOf(selectedEntry)
            val foundIndex = list.indexOf(activeItem)
            selectedEntry = activeItem
            notifyItemChanged(previousIndex)
            notifyItemChanged(foundIndex)
        }
    }

    class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        fun bindItem(name: String) {
            nameTextView.text = name
        }
    }
}