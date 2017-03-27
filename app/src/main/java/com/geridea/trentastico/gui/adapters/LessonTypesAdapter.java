package com.geridea.trentastico.gui.adapters;


/*
 * Created with ♥ by Slava on 27/03/2017.
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
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.utils.AppPreferences;

import java.util.Collection;

public class LessonTypesAdapter extends ItemsAdapter<LessonType> {

    public LessonTypesAdapter(Context context, Collection<LessonType> lessonTypes) {
        super(context);

        setItemsList(LessonType.getSortedLessonTypes(lessonTypes));
    }

    @Override
    protected View createView(LessonType item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_lesson_type, parent, false);
    }

    @Override
    protected void bindView(LessonType item, int pos, View convertView) {
        Views.<TextView>find(convertView, R.id.lesson_type).setText(item.getName());
        Views.<ImageView>find(convertView, R.id.color).setImageDrawable(new ColorDrawable(item.getColor()));

        if(AppPreferences.hasExtraCourseWithId(item.getId())){
            Views.find(convertView, R.id.course_already_selected).setVisibility(View.VISIBLE);
        } else {
            Views.find(convertView, R.id.course_already_selected).setVisibility(View.GONE);
        }
    }
}
