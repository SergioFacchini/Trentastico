package com.geridea.trentastico.gui.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.geridea.trentastico.R
import com.geridea.trentastico.model.StudyYear

class YearsAdapter(context: Context, years: List<StudyYear>) : ItemsAdapter<StudyYear>(context) {

    init {
        itemsList = years
    }

    override fun createView(
            item: StudyYear?,
            pos: Int,
            parent: ViewGroup,
            inflater: LayoutInflater): View = inflater.inflate(R.layout.itm_spinner, parent, false)

    override fun bindView(item: StudyYear, pos: Int, view: View) {
        (view.findViewById(R.id.text) as TextView).text = item.name
    }

    override fun getItemId(pos: Int): Long = pos.toLong()

    fun getPositionOfYearWithId(idToSearch: String): Int? =
            (0 until itemsList.size).firstOrNull { itemsList[it].id == idToSearch }
}
