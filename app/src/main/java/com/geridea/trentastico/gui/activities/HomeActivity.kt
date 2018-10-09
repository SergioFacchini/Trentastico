package com.geridea.trentastico.gui.activities

import android.content.Context
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.fragments.*
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterService
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.DebugUtils
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.dialog_trentastico_is_a_beta.view.*

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var currentFragment: Fragment? = null
    private var currentMenuSettings: IMenuSettings = NoMenuSettings.instance


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        //Showing the version
        val versionText = Views.find<TextView>(nav_view.getHeaderView(0), R.id.version_text)
        versionText.text = "Versione: " + DebugUtils.computeVersionName()

        //Removing debug stuff from menu
        if (!IS_IN_DEBUG_MODE) {
            val menu = nav_view.menu
            (0 until menu.size())
                    .map { menu.getItem(it) }
                    .filter {
                        //If we're not in debug mode, we don't need the debug options
                        it.itemId == R.id.debug_menu_about ||
                        it.itemId == R.id.debug_update_courses ||
                        it.itemId == R.id.debug_start_next_lesson_service ||
                        it.itemId == R.id.debug_reset_notification_tracker
                    }
                    .forEach { it.isVisible = false }
        }

        //Setting calendar fragment as the first fragment
        switchToCalendarFragment()

        //While the app is in beta we will show the user it's times
        if(!AppPreferences.wasAppInBetaMessageShown) {
            AppPreferences.wasAppInBetaMessageShown = true
            TrentasticoIsABetaDialog(this).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.fragment_calendar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /* Called whenever we call invalidateOptionsMenu() */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // If the nav drawer is open, hide all action items
        hideAllMenuItems(menu)

        for (id in currentMenuSettings.idsOfMenuItemsToMakeVisible) {
            val menuItem = menu.findItem(id)
            currentMenuSettings.bindMenuItem(menuItem)
            menuItem.isVisible = true
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun hideAllMenuItems(menu: Menu) {
        for (i in 0 until menu.size()) {
            menu.getItem(i).isVisible = false
        }
    }

    override fun onBackPressed() = if (drawer.isDrawerOpen(GravityCompat.START)) {
        drawer.closeDrawer(GravityCompat.START)
    } else {
        if (currentFragment is CalendarFragment) {
            super.onBackPressed()
        } else {
            switchToCalendarFragment()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when {
            id == R.id.menu_timetables  -> switchToCalendarFragment()
            id == R.id.menu_settings    -> setCurrentFragment(SettingsFragment())
            id == R.id.menu_extra_times -> setCurrentFragment(ExtraLessonsFragment())
            id == R.id.menu_libraries   -> setCurrentFragment(LibrariesFragment())
            id == R.id.menu_feedback    -> setCurrentFragment(SubmitFeedbackFragment())
            id == R.id.menu_changelog   -> setCurrentFragment(AboutFragment())
            IS_IN_DEBUG_MODE            -> //Managing debug stuff here
                when (id) {
                    R.id.debug_menu_about -> {
                        Networker.obliterateCache()
                        Toast.makeText(this, "Cache obliterated! :)", Toast.LENGTH_SHORT).show()
                        switchToCalendarFragment()
                    }
                    R.id.debug_update_courses            ->
                        startService(LessonsUpdaterService.createIntent(this, LessonsUpdaterService.STARTER_DEBUGGER))

                    R.id.debug_start_next_lesson_service ->
                        startService(NextLessonNotificationService.createIntent(this, NLNStarter.DEBUG))

                    R.id.debug_reset_notification_tracker -> {
                        AppPreferences.notificationTracker.clear()
                        Toast.makeText(this, "Notification tracker reset!", Toast.LENGTH_SHORT).show()
                    }

                }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun switchToCalendarFragment() {
        nav_view.setCheckedItem(R.id.menu_timetables)

        setCurrentFragment(CalendarFragment())
    }


    private fun setCurrentFragment(nextFragment: Fragment) {
        currentFragment = nextFragment

        currentMenuSettings = if (currentFragment is FragmentWithMenuItems) {
            val currentFragment = this.currentFragment as FragmentWithMenuItems?
            currentFragment!!.setActivity(this)
            currentFragment
        } else {
            NoMenuSettings.instance
        }

        invalidateOptionsMenu()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, nextFragment)
                .commit()
    }
}


internal class TrentasticoIsABetaDialog(context: Context) : AlertDialog(context) {

    init {
        val view = Views.inflate<View>(context, R.layout.dialog_trentastico_is_a_beta)
        view.okBtn.setOnClickListener { dismiss() }

        setView(view)
    }

}
