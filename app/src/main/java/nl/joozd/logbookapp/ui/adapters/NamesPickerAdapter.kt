package nl.joozd.logbookapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_names_dialog.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getColorFromAttr

class NamesPickerAdapter(var allNames: List<String>, private val itemClick: (String) -> Unit): RecyclerView.Adapter<NamesPickerAdapter.ViewHolder>() {
    private var currentNames: List<String>  = allNames // namesWorker.nameList
    private var selectedName: String? = null
    private var currentQuery: String = ""

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindName(currentNames[position])
        holder.itemBackground.setOnClickListener {
            selectedName = holder.nameTextView.text.toString()
                .also { itemClick(it) }
            notifyDataSetChanged()
        }
        holder.itemBackground.setBackgroundColor(if (holder.nameTextView.text.toString() == selectedName) holder.itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else holder.itemBackground.ctx.getColor(R.color.none) )
    }

    override fun getItemCount(): Int = currentNames.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NamesPickerAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_names_dialog, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        fun bindName(name: String) {
            nameTextView.text = name
        }
    }
    fun getNames(query: String){
        currentNames = queryNames(query)
        this.notifyDataSetChanged()
    }

    fun updateAllNames(names: List<String>){
        allNames = names
        currentNames = queryNames(currentQuery)
        this.notifyDataSetChanged()
    }


    /**
     * searches for hits in a list.
     * Saves query to [currentQuery] in case [allNames] changes, it will keep it's query on the new dataset
     * @return list containing all [query] hits, starting with hits at beginning, then the rest.
     */
    private fun queryNames(query: String): List<String> {
        currentQuery = query
        val startHits = allNames.filter {it.anyWordstartsWith(query)}
        return startHits + allNames.filter {it !in startHits}.filter{query in it}
    }

    private fun String.anyWordstartsWith(q: String): Boolean =
        this.split(" ").any{it.startsWith(q)}

}