package com.geridea.trentastico.gui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.alexvasilkov.android.commons.adapters.ItemsAdapter

import com.geridea.trentastico.R
import com.geridea.trentastico.model.Department
import com.geridea.trentastico.providers.DepartmentsProvider

class DepartmentsAdapter(context: Context) : ItemsAdapter<Department>(context) {

    init {

        DepartmentsProvider.loadIfNeeded()
        itemsList = DepartmentsProvider.DEPARTMENTS
    }

    override fun createView(item: Department, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View = inflater.inflate(R.layout.itm_spinner, parent, false)

    override fun bindView(item: Department, pos: Int, textView: View) {
        (textView.findViewById(R.id.text) as TextView).text = item.name
    }

    override fun getItemId(pos: Int): Long = getItem(pos).id.toLong()

}

