package com.geridea.trentastico.gui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.alexvasilkov.android.commons.adapters.ItemsAdapter

import com.geridea.trentastico.R
import com.geridea.trentastico.model.Course
import com.geridea.trentastico.model.Department

class CoursesAdapter(context: Context, department: Department) : ItemsAdapter<Course>(context) {

    init {

        itemsList = department.courses
    }

    override fun createView(item: Course, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View {
        return inflater.inflate(R.layout.itm_spinner, parent, false)
    }

    override fun bindView(item: Course, pos: Int, textView: View) {
        (textView.findViewById(R.id.text) as TextView).text = item.name
    }

    override fun getItemId(pos: Int): Long {
        return getItem(pos).id.toLong()
    }
}

