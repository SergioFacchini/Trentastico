package com.geridea.trentastico.gui.adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;
import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.R;
import com.geridea.trentastico.model.LessonType;
import com.geridea.trentastico.model.Partitioning;
import com.geridea.trentastico.model.PartitioningType;
import com.threerings.signals.Signal1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class CourseFilterAdapter extends ItemsAdapter<LessonType> {

    /**
     * Dispatched when the user clicked on the visibility checkbox of a lesson type, that means that
     * it's visibility has been changed and this have to be reflected on the calendar. The dispatched
     * LessonType already has it's visibility changed.
     */
    public final Signal1<LessonType> onLessonTypeVisibilityChanged = new Signal1<>();

    /**
     * Dispatched when the user clicks the button of configuration of a partitioning. The dispatched
     * item is the LessonType of which to check the partition.
     */
    public final Signal1<LessonType> onConfigurePartitioningButtonClicked = new Signal1<>();

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

        //Adjusting partitionings
        Partitioning partitioning = item.getPartitioning();
        TextView partitioningsTV = Views.find(convertView, R.id.partitionings);
        if (partitioning.getType() == PartitioningType.NONE) {
            partitioningsTV.setVisibility(View.GONE);

            Views.find(convertView, R.id.config_partitionings).setVisibility(View.GONE);
        } else {
            int size       = partitioning.getPartitioningCasesSize();
            int numVisible = partitioning.getNumVisiblePartitioningCases();

            partitioningsTV.setText(String.format(Locale.ITALIAN, "Mostrati %d gruppi su %s", numVisible, size));
            partitioningsTV.setVisibility(View.VISIBLE);

            ImageView configPartitionsButton = Views.find(convertView, R.id.config_partitionings);
            configPartitionsButton.setVisibility(View.VISIBLE);
            configPartitionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onConfigurePartitioningButtonClicked.dispatch(item);
                }
            });

        }

        Views.<ImageView>find(convertView, R.id.color).setImageDrawable(new ColorDrawable(item.getColor()));
    }
}
