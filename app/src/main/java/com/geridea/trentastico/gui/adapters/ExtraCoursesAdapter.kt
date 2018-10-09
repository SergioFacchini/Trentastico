package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 26/03/2017.
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
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.utils.ColorDispenser
import java.util.*

class ExtraCoursesAdapter(context: Context, extraCourses: ArrayList<ExtraCourse>) : ItemsAdapter<ExtraCourse>(context) {

    init {
        itemsList = extraCourses
    }

    override fun createView(item: ExtraCourse, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View
            = inflater.inflate(R.layout.itm_extra_course, parent, false)

    override fun bindView(item: ExtraCourse, pos: Int, convertView: View) {
        Views.find<TextView>(convertView, R.id.courseName).text = item.lessonName
        Views.find<TextView>(convertView, R.id.studyCourseName).text = item.fullName
        Views.find<TextView>(convertView, R.id.teacherName).text = item.buildTeachersNamesOrDefault()

        if (item.partitioningName != null) {
            Views.find<TextView>(convertView, R.id.partitioningName).text = item.partitioningName
            Views.find<TextView>(convertView, R.id.partitioningName).visibility = View.VISIBLE
        } else {
            Views.find<TextView>(convertView, R.id.partitioningName).visibility = View.GONE
        }

        val color = ColorDispenser.getColor(item.lessonTypeId)
        Views.find<ImageView>(convertView, R.id.color).setImageDrawable(ColorDrawable(color))

    }
}
