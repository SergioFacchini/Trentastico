package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 01/04/2017.
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.geridea.trentastico.R
import com.geridea.trentastico.network.request.LessonsDiffResult
import kotlinx.android.synthetic.main.itm_diff_result.view.*
import java.util.*

class DiffResultAdapter(context: Context, diffResult: LessonsDiffResult) : ItemsAdapter<DiffResultItem>(context) {

    init {

        val items = ArrayList<DiffResultItem>(diffResult.numTotalDifferences)

        diffResult.addedLessons  .mapTo(items) { DiffResultItem  .buildAdded(it) }
        diffResult.removedLessons.mapTo(items) { DiffResultItem.buildRemoved(it) }
        diffResult.changedLessons.mapTo(items) { DiffResultItem.buildChanged(it) }

        itemsList = items
    }

    override fun createView(item: DiffResultItem, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View
            = inflater.inflate(R.layout.itm_diff_result, parent, false)

    override fun bindView(item: DiffResultItem, pos: Int, view: View) {
        view.diff_type         .text = item.diffDescription
        view.courseName       .text = item.courseName
        view.scheduled_at_day  .text = "Pianificata per: " + item.scheduledDay
        view.scheduled_at_hours.text = "Alle ore: " + item.scheduledHours
        view.lesson_duration   .text = "Durata: " + item.duration.toString() + "min"

        if (item.modifications != null) {
            view.modifications.text       = item.modifications
            view.modifications.visibility = View.VISIBLE
        } else {
            view.modifications.visibility = View.GONE
        }

    }


}
