package com.geridea.trentastico.gui.adapters


/*
 * Created with ♥ by Slava on 25/03/2017.
 */

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.model.Partitioning
import com.geridea.trentastico.model.PartitioningCase
import com.threerings.signals.Signal1

import java.util.ArrayList

class PartitioningsAdapter(context: Context, partitionings: Partitioning) : ItemsAdapter<PartitioningCase>(context) {

    /**
     * Dispatched when the user clicked on the partition's checkbox.
     */
    val onPartitioningVisibilityChanged = Signal1<PartitioningCase>()

    init {

        itemsList = ArrayList(partitionings.sortedCases)
    }

    override fun createView(item: PartitioningCase, pos: Int, parent: ViewGroup, inflater: LayoutInflater): View {
        return inflater.inflate(R.layout.itm_partioning, parent, false)
    }

    override fun bindView(item: PartitioningCase, pos: Int, convertView: View) {
        val checkBox = Views.find<CheckBox>(convertView, R.id.partioning_name)
        checkBox.text = item.case
        checkBox.isChecked = item.isVisible
        checkBox.setOnClickListener {
            item.isVisible = checkBox.isChecked
            onPartitioningVisibilityChanged.dispatch(item)
        }
    }

}
