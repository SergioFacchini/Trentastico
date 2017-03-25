package com.geridea.trentastico.gui.adapters;


/*
 * Created with ♥ by Slava on 25/03/2017.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;
import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.model.Partitioning;
import com.geridea.trentastico.model.PartitioningCase;
import com.threerings.signals.Signal1;

import java.util.ArrayList;

public class PartitioningsAdapter extends ItemsAdapter<PartitioningCase> {

    /**
     * Dispatched when the user clicked on the partition's checkbox.
     */
    public final Signal1<PartitioningCase> onPartitioningVisibilityChanged = new Signal1<>();

    public PartitioningsAdapter(Context context, Partitioning partitionings) {
        super(context);

        setItemsList(new ArrayList<>(partitionings.getSortedCases()));
    }

    @Override
    protected View createView(PartitioningCase item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_partioning, parent, false);
    }

    @Override
    protected void bindView(final PartitioningCase item, int pos, View convertView) {
        final CheckBox checkBox = Views.find(convertView, R.id.partioning_name);
        checkBox.setText(item.getCase());
        checkBox.setChecked(item.isVisible());
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item.setVisible(checkBox.isChecked());
                onPartitioningVisibilityChanged.dispatch(item);
            }
        });
    }

}
