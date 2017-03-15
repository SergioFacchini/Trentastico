package com.geridea.trentastico.gui.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;

import java.util.ArrayList;

import com.geridea.trentastico.R;

public class YearsAdapter extends ItemsAdapter<Integer> {


    public YearsAdapter(Context context) {
        super(context);

        ArrayList<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(2);
        integers.add(3);
        integers.add(4);
        integers.add(5);

        setItemsList(integers);
    }

    @Override
    protected View createView(Integer item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_spinner, parent, false);
    }

    @Override
    protected void bindView(Integer item, int pos, View textView) {
        ((TextView) textView.findViewById(R.id.text)).setText(item.toString());
    }

    @Override
    public long getItemId(int pos) {
        return getItem(pos);
    }
}
