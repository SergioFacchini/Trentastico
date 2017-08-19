package com.geridea.trentastico.gui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.alexvasilkov.android.commons.adapters.ItemsAdapter

import com.geridea.trentastico.R
import com.geridea.trentastico.model.Course

class CoursesAdapter(context: Context, courses: List<Course>) : ItemsAdapter<Course>(context) {

    init {
        itemsList = courses
    }

    override fun createView(item: Course, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View = inflater.inflate(R.layout.itm_spinner, parent, false)

    override fun bindView(item: Course, pos: Int, textView: View) {
        (textView.findViewById(R.id.text) as TextView).text = item.name
    }

    override fun getItemId(pos: Int): Long = pos.toLong()

    fun getPositionOfCourseWithId(courseId: String): Int? =
        (0 until itemsList.size).firstOrNull { itemsList[it].id == courseId }
}

