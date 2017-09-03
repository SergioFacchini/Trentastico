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

    init {
        itemsList = lessons.sortedWith(Comparator { a, b ->  a.name.compareTo(b.name)})
    }

    override fun createView(item: LessonTypeNew, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View
            = inflater.inflate(R.layout.itm_course, parent, false)

    override fun bindView(item: LessonTypeNew, pos: Int, convertView: View) {
        Views.find<ImageView>(convertView, R.id.color).setImageDrawable(ColorDrawable(item.color))

        Views.find<TextView>(convertView, R.id.lesson_type).text    = item.name
        Views.find<TextView>(convertView, R.id.teachers_names).text = item.teachersNames

        val partitioningNameTV = Views.find<TextView>(convertView, R.id.partitioning_name)
        if (item.partitioningName != null) {
            partitioningNameTV.text = item.partitioningName
            partitioningNameTV.visibility = View.VISIBLE
        } else {
            partitioningNameTV.visibility = View.GONE
        }

        val check = Views.find<CheckBox>(convertView, R.id.checkBox)
        check.setOnCheckedChangeListener {  _, isChecked: Boolean -> Unit }

        check.isChecked = item.isVisible
        check.setOnCheckedChangeListener { _, isChecked: Boolean ->
            item.isVisible = check.isChecked
            onLessonTypeVisibilityChanged.dispatch(item)
        }

        convertView.setOnClickListener {
            check.toggle()
        }
    }
}