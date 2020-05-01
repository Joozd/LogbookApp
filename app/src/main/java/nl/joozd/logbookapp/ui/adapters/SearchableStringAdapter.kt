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
import java.util.*

/**
 * Adapter for RecyclerView that can search it's values, and highlight one of them.
 * Needs
 * @param clickSetsSelection: if set to true, onClick on an item sets it as [selectedEntry] in
 * addition to running [itemClick].
 * @param scrollToSelectedEntry: If set to true, makes the RecyclerView this is attached to scroll
 * to the position of the selected item
 * @param color: the color of [itemBackGround] will be set to this value for [selectedEntry]
 * @param itemLayout: Layout to be used for an item. MUST have a view named [itemBackGround]
 * @param itemClick: Action to be performed onClick on an item
 *
 *
 */
@Deprecated ("This no longer falls within design specs")
class SearchableStringAdapter(
    var allData: List<String>,
    var clickSetsSelection: Boolean = false,
    var scrollToSelectedEntry: Boolean = false,
    var color: Int? = null,
    val itemLayout: Int = R.layout.item_picker_dialog,
    private val itemClick: (String) -> Unit
): RecyclerView.Adapter<SearchableStringAdapter.ViewHolder>() {

    private var foundEntries: List<String>  = allData // namesWorker.nameList
    private var selectedEntry: String? = null
    private var currentQuery: String = ""
    private lateinit var mRecyclerView: RecyclerView



    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindName(foundEntries[position])
        holder.itemBackground.setOnClickListener {
            if (clickSetsSelection) {
                selectedEntry = holder.nameTextView.text.toString()
                    .also { itemClick(it) }
                notifyItemChanged(position)
            }
            else itemClick(holder.nameTextView.text.toString())
        }
        holder.itemBackground.setBackgroundColor(if (holder.nameTextView.text.toString() == selectedEntry) color?: holder.itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else holder.itemBackground.ctx.getColor(R.color.none) )
    }

    override fun getItemCount(): Int = foundEntries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_picker_dialog, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        fun bindName(name: String) {
            nameTextView.text = name
        }
    }
    fun search(query: String){
        foundEntries = queryEntries(query)
        this.notifyDataSetChanged()
    }

    fun updateAllData(data: List<String>){
        allData = data
        foundEntries = queryEntries(currentQuery)
        this.notifyDataSetChanged()
    }

    fun selectActiveItem(activeItem: String){
        if (activeItem in foundEntries) {
            val previousIndex = foundEntries.indexOf(selectedEntry)
            val foundIndex = foundEntries.indexOf(activeItem)
            selectedEntry = activeItem
            notifyItemChanged(previousIndex)
            notifyItemChanged(foundIndex)
            if (scrollToSelectedEntry)
                mRecyclerView.scrollToPosition((foundIndex + 5))
        }
    }


    /**
     * searches for hits in a list.
     * Saves query to [currentQuery] in case [allData] changes, it will keep it's query on the new dataset
     * @return list containing all [query] hits, starting with hits at beginning, then the rest.
     */
    private fun queryEntries(query: String): List<String> {
        currentQuery = query.toUpperCase(Locale.ROOT)
        val startHits =
            allData.map { it}.filter {it.toUpperCase(Locale.ROOT).anyWordstartsWith(currentQuery)}
        return startHits + allData.filter {it !in startHits}.filter{currentQuery in it.toUpperCase(Locale.ROOT)}
    }

    private fun String.anyWordstartsWith(q: String): Boolean =
        this.split(" ").any{it.startsWith(q)}

}