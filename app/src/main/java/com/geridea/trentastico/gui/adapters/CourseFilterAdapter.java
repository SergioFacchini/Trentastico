package com.geridea.trentastico.gui.adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;
import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.model.LessonType;
import com.threerings.signals.Signal1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class CourseFilterAdapter extends ItemsAdapter<LessonType> {

    /**
     * Dispatched when the user clicked on the visibility checkbox of a lesson type, that means that
     * it's visibility has been changed and this have to be reflected on the calendar. The dispatched
     * LessonType already has it's visibility changed.
     */
    public final Signal1<LessonType> onLessonTypeVisibilityChanged = new Signal1<>();

    public CourseFilterAdapter(Context context, Collection<LessonType> lessons) {
        super(context);

        ArrayList<LessonType> lessonTypes = new ArrayList<>(lessons);

        //Sort all the courses alphabetically
        Collections.sort(lessonTypes, new Comparator<LessonType>() {
            @Override
            public int compare(LessonType o1, LessonType o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        setItemsList(lessonTypes);
    }

    @Override
    protected View createView(LessonType item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_course, parent, false);
    }

    @Override
    protected void bindView(final LessonType item, int pos, View convertView) {
        final CheckBox check = Views.find(convertView, R.id.checkBox);
        check.setText(item.getName());
        check.setChecked(item.isVisible());

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item.setVisible(check.isChecked());
                onLessonTypeVisibilityChanged.dispatch(item);
            }
        });

        Views.<ImageView>find(convertView, R.id.color).setImageDrawable(new ColorDrawable(item.getColor()));
    }
}
