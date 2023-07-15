package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.findTextView


/**
 * Adapter for RecyclerView that can swipe entries
 * Needs
 * @param itemLayout: Layout to be used for an item. MUST have a view named 'item_background'
 * @param callBack: The callback for the swiping stuff
 */
class SwipableStringAdapter(
    private val itemLayout: Int = R.layout.item_picker_dialog,
    private val callBack: ItemTouchHelper.Callback
): ListAdapter<String, SwipableStringAdapter.StringViewHolder>(DIFF_CALLBACK) {

    // Secondary constructor takes context to instantiate SwipeToDeleteCallback
    constructor(
        context: Context,
        itemLayout: Int = R.layout.item_picker_dialog,
        onSwipe: OnSwipeListener
    ): this(itemLayout, SwipeToDeleteCallback(context, onSwipe))


    private var _viewActions: (View.(String?)-> Unit)? = null

    fun viewActions(actions: View.(String?)-> Unit){
        this._viewActions = actions
    }

    class StringViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val textView = itemView.findTextView()
        var text: String? get() = textView?.text?.toString()
            set(t) { textView?.text = t }
        val view = itemView
        var index: Int = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayout, parent, false)
        return StringViewHolder(view)
    }

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        holder.apply {
            text = getItem(position)
            index = position
            _viewActions?.let { actions ->
                view.apply {
                    actions(holder.text)
                }
            }
        }
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if(callBack is SwipeToDeleteCallback) callBack.adapter = this
        ItemTouchHelper(callBack).attachToRecyclerView(recyclerView)
    }

    open class SwipeToDeleteCallback(private val context: Context, private val onSwipe: OnSwipeListener) : ItemTouchHelper.Callback() {
        private val clearPaint: Paint = Paint()
        private val backgroundColorDrawable: ColorDrawable = ColorDrawable()
        private val backgroundColor: Int = Color.parseColor("#b80f0a")
        private val deleteDrawable = ContextCompat.getDrawable(context, android.R.drawable.ic_delete) ?: emptyDrawable()
        private val intrinsicWidth = deleteDrawable.intrinsicWidth
        private val intrinsicHeight = deleteDrawable.intrinsicHeight

        var adapter: SwipableStringAdapter? = null

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            //only allow swipe left
            return makeMovementFlags(0, ItemTouchHelper.LEFT)
        }


        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, viewHolder1: RecyclerView.ViewHolder): Boolean {
            // not used
            return false
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            val itemView = viewHolder.itemView
            val itemHeight = itemView.height
            val isCancelled = dX == 0f && !isCurrentlyActive
            if (isCancelled) {
                clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                return
            }
            backgroundColorDrawable.color = backgroundColor
            backgroundColorDrawable.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
            backgroundColorDrawable.draw(c)
            val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
            val deleteIconLeft = itemView.right - deleteIconMargin - intrinsicWidth
            val deleteIconRight = itemView.right - deleteIconMargin
            val deleteIconBottom = deleteIconTop + intrinsicHeight
            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
            deleteDrawable.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        private fun clearCanvas(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
            c.drawRect(left, top, right, bottom, clearPaint)
        }

        override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
            return 0.7f
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            viewHolder.itemView.translationX = 0f
            adapter?.notifyItemChanged(viewHolder.absoluteAdapterPosition)
            onSwipe(viewHolder.findText())
        }

        private fun emptyDrawable(size: Int = 24) = ShapeDrawable(OvalShape()).apply{
            paint.color = context.getColor(android.R.color.transparent)
            intrinsicHeight = size
            intrinsicWidth = size
        }

        private fun RecyclerView.ViewHolder.findText(): String =
            itemView.findTextView()?.text?.toString() ?: error ("cannot find text in $this")
    }

    fun interface OnSwipeListener{
        operator fun invoke(swipedString: String)
    }

    companion object{
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }

}