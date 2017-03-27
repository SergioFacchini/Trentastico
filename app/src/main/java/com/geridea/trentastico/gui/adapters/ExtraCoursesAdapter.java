package com.geridea.trentastico.gui.adapters;


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;
import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.model.ExtraCourse;

import java.util.ArrayList;

public class ExtraCoursesAdapter extends ItemsAdapter<ExtraCourse> {

    public ExtraCoursesAdapter(Context context, ArrayList<ExtraCourse> extraCourses) {
        super(context);

        setItemsList(extraCourses);
    }

    @Override
    protected View createView(ExtraCourse item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_extra_course, parent, false);
    }

    @Override
    protected void bindView(ExtraCourse item, int pos, View convertView) {
        Views.<TextView>find(convertView, R.id.course_name).setText(item.getName());
        Views.<TextView>find(convertView, R.id.study_course_name).setText(item.getStudyCourseFullName());

        Views.<ImageView>find(convertView, R.id.color).setImageDrawable(new ColorDrawable(item.getColor()));

    }
}
