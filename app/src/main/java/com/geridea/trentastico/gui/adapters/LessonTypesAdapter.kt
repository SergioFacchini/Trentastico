package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.utils.AppPreferences

class LessonTypesAdapter(
        context: Context,
        lessonTypes: Collection<LessonType>,
        private val alreadyTakenLessons: List<LessonType>) : ItemsAdapter<LessonType>(context) {

    init {
        itemsList = lessonTypes.sortedBy { it.name }
    }

    override fun createView(item: LessonType, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View =
            inflater.inflate(R.layout.itm_lesson_type, parent, false)

    override fun bindView(item: LessonType, pos: Int, convertView: View) {
        Views.find<TextView> (convertView, R.id.lesson_type).text    = item.name
        Views.find<TextView> (convertView, R.id.teachers_names).text = item.buildTeachersNamesOrDefault()

        if (item.partitioningName != null) {
            Views.find<TextView> (convertView, R.id.partitioning_name).text = item.partitioningName
            Views.find<TextView> (convertView, R.id.partitioning_name).visibility = View.VISIBLE
        } else {
            Views.find<TextView> (convertView, R.id.partitioning_name).visibility = View.GONE
        }

        if (AppPreferences.hasExtraCourseWithId(item.id) || alreadyTakenLessons.any { it.id == item.id }) {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.VISIBLE
        } else {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.GONE
        }
    }
}
