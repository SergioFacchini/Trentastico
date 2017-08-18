package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 01/04/2017.
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
import com.geridea.trentastico.network.request.LessonsDiffResult
import java.util.*

class DiffResultAdapter(context: Context, diffResult: LessonsDiffResult) : ItemsAdapter<DiffResultItem>(context) {

    init {

        val items = ArrayList<DiffResultItem>(diffResult.numTotalDifferences)
        for (lesson in diffResult.addedLessons) {
            items.add(DiffResultItem.buildAdded(lesson))
        }

        for (lesson in diffResult.removedLessons) {
            items.add(DiffResultItem.buildRemoved(lesson))
        }

        for (change in diffResult.changedLessons) {
            items.add(DiffResultItem.buildChanged(change))
        }

        itemsList = items
    }

    override fun createView(item: DiffResultItem, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View = inflater.inflate(R.layout.itm_diff_result, parent, false)

    override fun bindView(item: DiffResultItem, pos: Int, view: View) {
        Views.find<TextView>(view, R.id.diff_type).text = item.diffDescription
        Views.find<TextView>(view, R.id.course_name).text = item.courseName
        Views.find<TextView>(view, R.id.scheduled_at_day).text = "Pianificata per: " + item.scheduledDay
        Views.find<TextView>(view, R.id.scheduled_at_hours).text = "Alle ore: " + item.scheduledHours
        Views.find<TextView>(view, R.id.lesson_duration).text = "Durata: " + item.duration.toString() + "min"

        val modificationsTV = Views.find<TextView>(view, R.id.modifications)
        if (item.modifications != null) {
            modificationsTV.text = item.modifications
            modificationsTV.visibility = View.VISIBLE
        } else {
            modificationsTV.visibility = View.GONE
        }

        Views.find<ImageView>(view, R.id.color).setImageDrawable(ColorDrawable(item.color))

    }


}
