package com.geridea.trentastico.gui.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.model.LessonTypeNew
import com.threerings.signals.Signal1
import java.util.*

class CourseFilterAdapter(context: Context, lessons: Collection<LessonTypeNew>) : ItemsAdapter<LessonTypeNew>(context) {

    /**
     * Dispatched when the user clicked on the visibility checkbox of a lesson type, that means that
     * it's visibility has been changed and this have to be reflected on the calendar. The dispatched
     * LessonType already has it's visibility changed.
     */
    val onLessonTypeVisibilityChanged = Signal1<LessonTypeNew>()

    /**
     * Dispatched when the user clicks the button of configuration of a partitioning. The dispatched
     * item is the LessonType of which to check the partition.
     */
    val onConfigurePartitioningButtonClicked = Signal1<LessonTypeNew>()

    init {
        itemsList = lessons.sortedWith(Comparator { a, b ->  a.name.compareTo(b.name)})
    }

    override fun createView(item: LessonTypeNew, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View
            = inflater.inflate(R.layout.itm_course, parent, false)

    override fun bindView(item: LessonTypeNew, pos: Int, convertView: View) {
        val check = Views.find<CheckBox>(convertView, R.id.checkBox)
        check.text = item.name + "\n" + item.teachersNames
        check.isChecked = item.isVisible
        check.setOnClickListener {
            item.isVisible = check.isChecked
            onLessonTypeVisibilityChanged.dispatch(item)
        }

        //Adjusting partitionings
        //TODO: implement partitionings
//        val partitioning = item.partitioning
        val partitioningsTV = Views.find<TextView>(convertView, R.id.partitionings)
//        if (partitioning.type == PartitioningType.NONE) {
            partitioningsTV.visibility = View.GONE

            Views.find<View>(convertView, R.id.config_partitionings).visibility = View.GONE
//        } else {
//            val size = partitioning.partitioningCasesSize
//            val numVisible = partitioning.numVisiblePartitioningCases
//
//            partitioningsTV.text = String.format(Locale.ITALIAN, "Mostrati %d gruppi su %s", numVisible, size)
//            partitioningsTV.visibility = View.VISIBLE
//
//            val configPartitionsButton = Views.find<ImageView>(convertView, R.id.config_partitionings)
//            configPartitionsButton.visibility = View.VISIBLE
//            configPartitionsButton.setOnClickListener { onConfigurePartitioningButtonClicked.dispatch(item) }

//        }

        Views.find<ImageView>(convertView, R.id.color).setImageDrawable(ColorDrawable(item.color))
    }
}
