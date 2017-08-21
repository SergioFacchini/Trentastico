package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 27/03/2017.
 */

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.model.LessonTypeNew
import com.geridea.trentastico.utils.AppPreferences

class LessonTypesAdapter(context: Context, lessonTypes: Collection<LessonTypeNew>) : ItemsAdapter<LessonTypeNew>(context) {

    init {
        itemsList = lessonTypes.sortedBy { it.name }
    }

    override fun createView(item: LessonTypeNew, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View =
            inflater.inflate(R.layout.itm_lesson_type, parent, false)

    override fun bindView(item: LessonTypeNew, pos: Int, convertView: View) {
        Views.find<TextView> (convertView, R.id.lesson_type).text    = item.name
        Views.find<TextView> (convertView, R.id.teachers_names).text = item.teachersNames

        if (item.partitioningName != null) {
            Views.find<TextView> (convertView, R.id.partitioning_name).text = item.partitioningName
            Views.find<TextView> (convertView, R.id.partitioning_name).visibility = View.VISIBLE
        } else {
            Views.find<TextView> (convertView, R.id.partitioning_name).visibility = View.GONE
        }

        Views.find<ImageView>(convertView, R.id.color).setImageDrawable(ColorDrawable(item.color))

        if (AppPreferences.hasExtraCourseWithId(item.id)) {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.VISIBLE
        } else {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.GONE
        }
    }
}
