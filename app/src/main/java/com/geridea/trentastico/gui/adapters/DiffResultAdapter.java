package com.geridea.trentastico.gui.adapters;


/*
 * Created with ♥ by Slava on 01/04/2017.
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
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.network.request.LessonsDiffResult;

import java.util.ArrayList;

public class DiffResultAdapter extends ItemsAdapter<DiffResultItem> {

    public DiffResultAdapter(Context context, LessonsDiffResult diffResult) {
        super(context);

        ArrayList<DiffResultItem> items = new ArrayList<>(diffResult.getNumTotalDifferences());
        for (LessonSchedule lesson : diffResult.getAddedLessons()) {
            items.add(DiffResultItem.buildAdded(lesson));
        }

        for (LessonSchedule lesson : diffResult.getRemovedLessons()) {
            items.add(DiffResultItem.buildRemoved(lesson));
        }

        for (LessonsDiffResult.LessonChange change : diffResult.getChangedLessons()) {
            items.add(DiffResultItem.buildChanged(change));
        }

        setItemsList(items);
    }

    @Override
    protected View createView(DiffResultItem item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_diff_result, parent, false);
    }

    @Override
    protected void bindView(DiffResultItem item, int pos, View view) {
        Views.<TextView>find(view, R.id.diff_type)         .setText(item.getDiffDescription());
        Views.<TextView>find(view, R.id.course_name)       .setText(item.getCourseName());
        Views.<TextView>find(view, R.id.scheduled_at_day)  .setText(
                "Pianificata per: "+item.getScheduledDay()
        );
        Views.<TextView>find(view, R.id.scheduled_at_hours).setText(
                "Alle ore: "+item.getScheduledHours()
        );
        Views.<TextView>find(view, R.id.lesson_duration)   .setText(
                "Durata: "+String.valueOf(item.getDuration())+"min"
        );

        TextView modificationsTV = Views.find(view, R.id.modifications);
        if(item.hasModifications()){
            modificationsTV.setText(item.getModifications());
            modificationsTV.setVisibility(View.VISIBLE);
        } else {
            modificationsTV.setVisibility(View.GONE);
        }

        Views.<ImageView>find(view, R.id.color).setImageDrawable(new ColorDrawable(item.getColor()));

    }


}
