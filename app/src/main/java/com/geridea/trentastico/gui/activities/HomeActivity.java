package com.geridea.trentastico.gui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.android.commons.utils.Views;
import com.geridea.trentastico.BuildConfig;
import com.geridea.trentastico.Config;
import com.geridea.trentastico.R;
import com.geridea.trentastico.database.Cacher;
import com.geridea.trentastico.gui.fragments.CalendarFragment;
import com.geridea.trentastico.gui.fragments.ExtraLessonsFragment;
import com.geridea.trentastico.gui.fragments.SettingsFragment;
import com.geridea.trentastico.gui.fragments.SubmitFeedbackFragment;
import com.geridea.trentastico.services.LessonsUpdaterService;
import com.geridea.trentastico.services.NextLessonNotificationService;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.toolbar)       Toolbar toolbar;
    @BindView(R.id.drawer_layout) DrawerLayout drawer;
    @BindView(R.id.nav_view)      NavigationView navigationView;

    private IFragmentWithMenuItems currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        //Showing the version
        TextView versionText = Views.find(navigationView.getHeaderView(0), R.id.version_text);
        versionText.setText(computeVersionName());

        //Removing debug stuff from menu
        if (!Config.DEBUG_MODE) {
            Menu menu = navigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);

                //If we're not in debug mode, we don't need the debug options
                if(item.getItemId() == R.id.debug_menu_about ||
                        item.getItemId() == R.id.debug_update_courses ||
                        item.getItemId() == R.id.debug_start_next_lesson_service) {

                    item.setVisible(false);
                }
            }
        }

        //Setting calendar fragment as the first fragment
        switchToCalendarFragment();
    }

    private String computeVersionName() {
        return String.format("Versione: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_calendar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide all action items
        hideAllMenuItems(menu);

        for (int id: currentFragment.getIdsOfMenuItemsToMakeVisible()) {
            MenuItem menuItem = menu.findItem(id);
            currentFragment.bindMenuItem(menuItem);
            menuItem.setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void hideAllMenuItems(Menu menu) {
        for(int i = 0; i<menu.size(); i++){
            menu.getItem(i).setVisible(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (currentFragment instanceof CalendarFragment) {
                super.onBackPressed();
            } else {
                switchToCalendarFragment();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.menu_timetables){
            switchToCalendarFragment();
        } else if(id == R.id.menu_settings) {
            setCurrentFragment(new SettingsFragment());
        } else if(id == R.id.menu_extra_times) {
            setCurrentFragment(new ExtraLessonsFragment());
        } else if(id == R.id.menu_feedback){
            setCurrentFragment(new SubmitFeedbackFragment());
            throw new RuntimeException("JUST A TEST!");
        } else if(Config.DEBUG_MODE){

            //Managing debug stuff here
            if(id == R.id.debug_menu_about){
                Cacher.obliterateCache();
                Toast.makeText(this, "Cache obliterated! :)", Toast.LENGTH_SHORT).show();
                switchToCalendarFragment();
            } else if(id == R.id.debug_update_courses){
                startService(LessonsUpdaterService.createIntent(this, LessonsUpdaterService.STARTER_DEBUGGER));
            } else if(id == R.id.debug_start_next_lesson_service){
                startService(NextLessonNotificationService.createIntent(this, NextLessonNotificationService.STARTER_DEBUG));
            }
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void switchToCalendarFragment() {
        navigationView.setCheckedItem(R.id.menu_timetables);

        setCurrentFragment(new CalendarFragment());
    }


    private void setCurrentFragment(IFragmentWithMenuItems nextFragment) {
        currentFragment = nextFragment;
        currentFragment.setActivity(this);

        invalidateOptionsMenu();

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, nextFragment)
                .commit();
    }
}
