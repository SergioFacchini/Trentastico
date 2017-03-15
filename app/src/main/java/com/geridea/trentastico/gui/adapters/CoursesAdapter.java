package com.geridea.trentastico.gui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;

import com.geridea.trentastico.R;
import com.geridea.trentastico.model.Course;
import com.geridea.trentastico.model.Department;

public class CoursesAdapter extends ItemsAdapter<Course> {

    public CoursesAdapter(Context context, Department department) {
        super(context);

        setItemsList(department.getCourses());
    }

    @Override
    protected View createView(Course item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_spinner, parent, false);
    }

    @Override
    protected void bindView(Course item, int pos, View textView) {
        ((TextView) textView.findViewById(R.id.text)).setText(item.getName());
    }

    @Override
    public long getItemId(int pos) {
        return getItem(pos).getId();
    }
}

