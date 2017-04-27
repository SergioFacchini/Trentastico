package com.geridea.trentastico.gui.fragments;


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.geridea.trentastico.BuildConfig;
import com.geridea.trentastico.R;
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems;
import com.geridea.trentastico.utils.DebugUtils;
import com.github.porokoro.paperboy.PaperboyFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutFragment extends FragmentWithMenuItems {

    @BindView(R.id.version_text) TextView versionText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, view);

        versionText.setText(DebugUtils.computeVersionName());

        PaperboyFragment paperboyFragment = PaperboyFragmentMaker.buildPaperboyFragment(getContext());
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.paperboy_fragment_frame, paperboyFragment)
            .commit();

        return view;
    }

    @Override
    public int[] getIdsOfMenuItemsToMakeVisible() {
        return new int[0];
    }

    @Override
    public void bindMenuItem(MenuItem item) {
        //No menus used
    }

}
