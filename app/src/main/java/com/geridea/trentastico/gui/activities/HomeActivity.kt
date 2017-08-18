package com.geridea.trentastico.gui.activities

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.Config
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.fragments.*
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.LessonsUpdaterService
import com.geridea.trentastico.services.NLNStarter
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.DebugUtils

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.toolbar)       lateinit var toolbar: Toolbar
    @BindView(R.id.drawer_layout) lateinit var drawer: DrawerLayout
    @BindView(R.id.nav_view)      lateinit var navigationView: NavigationView

    private var currentFragment: Fragment? = null
    private var currentMenuSettings: IMenuSettings = NoMenuSettings.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ButterKnife.bind(this)

        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.setDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        //Showing the version
        val versionText = Views.find<TextView>(navigationView.getHeaderView(0), R.id.version_text)
        versionText.text = "Versione: ${DebugUtils.computeVersionName()}"

        //Removing debug stuff from menu
        if (!Config.DEBUG_MODE) {
            val menu = navigationView.menu
            for (i in 0..menu.size() - 1) {
                val item = menu.getItem(i)

                //If we're not in debug mode, we don't need the debug options
                if (item.itemId == R.id.debug_menu_about ||
                        item.itemId == R.id.debug_update_courses ||
                        item.itemId == R.id.debug_start_next_lesson_service) {

                    item.isVisible = false
                }
            }
        }

        //Setting calendar fragment as the first fragment
        switchToCalendarFragment()
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
        for (i in 0..menu.size() - 1) {
            menu.getItem(i).isVisible = false
        }
    }

    override fun onBackPressed() = if (drawer!!.isDrawerOpen(GravityCompat.START)) {
        drawer!!.closeDrawer(GravityCompat.START)
    } else {
        if (currentFragment is CalendarFragment) {
            super.onBackPressed()
        } else {
            switchToCalendarFragment()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.menu_timetables) {
            switchToCalendarFragment()
        } else if (id == R.id.menu_settings) {
            setCurrentFragment(SettingsFragment())
        } else if (id == R.id.menu_extra_times) {
            setCurrentFragment(ExtraLessonsFragment())
        } else if (id == R.id.menu_libraries) {
            setCurrentFragment(LibrariesFragment())
        } else if (id == R.id.menu_feedback) {
            setCurrentFragment(SubmitFeedbackFragment())
        } else if (id == R.id.menu_changelog) {
            setCurrentFragment(AboutFragment())
        } else if (Config.DEBUG_MODE) {

            //Managing debug stuff here
            if (id == R.id.debug_menu_about) {
                Networker.obliterateLessonsCache()
                Toast.makeText(this, "Cache obliterated! :)", Toast.LENGTH_SHORT).show()
                switchToCalendarFragment()
            } else if (id == R.id.debug_update_courses) {
                startService(LessonsUpdaterService.createIntent(this, LessonsUpdaterService.STARTER_DEBUGGER))
            } else if (id == R.id.debug_start_next_lesson_service) {
                startService(NextLessonNotificationService.createIntent(this, NLNStarter.DEBUG))
            }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun switchToCalendarFragment() {
        navigationView.setCheckedItem(R.id.menu_timetables)

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
