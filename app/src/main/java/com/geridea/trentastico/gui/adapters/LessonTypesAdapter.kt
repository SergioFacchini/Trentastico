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
import com.geridea.trentastico.model.LessonType
import com.geridea.trentastico.utils.AppPreferences

class LessonTypesAdapter(context: Context, lessonTypes: Collection<LessonType>) : ItemsAdapter<LessonType>(context) {

    init {

        itemsList = LessonType.getSortedLessonTypes(lessonTypes)
    }

    override fun createView(item: LessonType, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View {
        return inflater.inflate(R.layout.itm_lesson_type, parent, false)
    }

    override fun bindView(item: LessonType, pos: Int, convertView: View) {
        Views.find<TextView>(convertView, R.id.lesson_type).text = item.name
        Views.find<ImageView>(convertView, R.id.color).setImageDrawable(ColorDrawable(item.color))

        if (AppPreferences.hasExtraCourseWithId(item.id)) {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.VISIBLE
        } else {
            Views.find<View>(convertView, R.id.course_already_selected).visibility = View.GONE
        }
    }
}
