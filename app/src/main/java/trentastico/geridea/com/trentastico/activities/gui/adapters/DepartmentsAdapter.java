package trentastico.geridea.com.trentastico.activities.gui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alexvasilkov.android.commons.adapters.ItemsAdapter;

import trentastico.geridea.com.trentastico.R;
import trentastico.geridea.com.trentastico.activities.model.Department;
import trentastico.geridea.com.trentastico.activities.providers.DepartmentsProvider;

public class DepartmentsAdapter extends ItemsAdapter<Department> {

    public DepartmentsAdapter(Context context) {
        super(context);

        DepartmentsProvider.loadIfNeeded();
        setItemsList(DepartmentsProvider.DEPARTMENTS);
    }

    @Override
    protected View createView(Department item, int pos, ViewGroup parent, LayoutInflater inflater) {
        return inflater.inflate(R.layout.itm_spinner, parent, false);
    }

    @Override
    protected void bindView(Department item, int pos, View textView) {
        ((TextView) textView.findViewById(R.id.text)).setText(item.getName());
    }

    @Override
    public long getItemId(int pos) {
        return getItem(pos).getId();
    }

}

