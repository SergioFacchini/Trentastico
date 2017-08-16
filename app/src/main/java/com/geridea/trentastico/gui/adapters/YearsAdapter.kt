package com.geridea.trentastico.gui.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.alexvasilkov.android.commons.adapters.ItemsAdapter

import java.util.ArrayList

import com.geridea.trentastico.R

class YearsAdapter(context: Context) : ItemsAdapter<Int>(context) {


    init {

        val integers = ArrayList<Int>()
        integers.add(1)
        integers.add(2)
        integers.add(3)
        integers.add(4)
        integers.add(5)

        itemsList = integers
    }

    override fun createView(item: Int?, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View {
        return inflater.inflate(R.layout.itm_spinner, parent, false)
    }

    override fun bindView(item: Int?, pos: Int, textView: View) {
        (textView.findViewById(R.id.text) as TextView).text = item!!.toString()
    }

    override fun getItemId(pos: Int): Long {
        return getItem(pos).toLong()
    }
}
