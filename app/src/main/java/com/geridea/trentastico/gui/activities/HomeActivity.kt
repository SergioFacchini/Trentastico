package com.geridea.trentastico.gui.activities

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.ViewCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.billing.BillingManager
import com.geridea.trentastico.gui.activities.dialog.DonateDialog
import com.geridea.trentastico.gui.fragments.*
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.findDonationItemByInternalId
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.services.DonationPopupManager
import com.geridea.trentastico.services.LessonsUpdaterJob
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import com.geridea.trentastico.utils.DebugUtils
import com.geridea.trentastico.utils.IS_IN_DEBUG_MODE
import com.geridea.trentastico.utils.copyText
import com.hypertrack.hyperlog.HyperLog
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.nav_header_home.view.*


class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var currentFragment: Fragment? = null
    private var currentMenuSettings: IMenuSettings = NoMenuSettings.instance

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupToolbar()
        setupActionBarDrawer()
        setupMenuHeader()
        setupMenuForNonDebugMode()

        switchToCalendarFragment()

        setBillingProcessor()
        showDonatePopupIfNeeded()
    }

    private fun showDonatePopupIfNeeded() {
        if (DonationPopupManager.shouldPopupBeShown(this)) {
            BugLogger.info("Showing donation dialog", "DONATE")
            ViewCompat.postOnAnimationDelayed(nav_view, {
                showDonateDialog()
                DonationPopupManager.rescheduleNotification()
            }, 10*1000)
        } else {
            BugLogger.info("Too soon to show the donation dialog", "DONATE")
        }
    }

    private fun setupMenuForNonDebugMode() {
        if (!IS_IN_DEBUG_MODE) {
            val menu = nav_view.menu
            (0 until menu.size())
                    .map { menu.getItem(it) }
                    .filter {
                        //If we're not in debug mode, we don't need the debug options
                        it.itemId in listOf(
                                R.id.debug_menu_about,
                                R.id.debug_update_courses,
                                R.id.debug_start_next_lesson_service,
                                R.id.debug_reset_notification_tracker,
                                R.id.debug_show_logs)
                    }
                    .forEach { it.isVisible = false }
        }
    }

    private fun setBillingProcessor() {
        billingManager = BillingManager(this)
        billingManager.init()
    }

    override fun onDestroy() {
        billingManager.release()
        super.onDestroy()
    }

    private fun setupMenuHeader() {
        //Showing the version
        val versionText = Views.find<TextView>(nav_view.getHeaderView(0), R.id.version_text)
        versionText.text = "Versione: " + DebugUtils.computeVersionName()

        //Showing the donation icon
        val donationIconId = AppPreferences.donationIconId
        if(donationIconId != null){
            val donationItem = findDonationItemByInternalId(donationIconId)
            setDrawerHeaderForDonation(donationItem.resourceHeader)
        }
    }

    private fun setDrawerHeaderForDonation(@DrawableRes resource: Int) {
        val donationImage = ResourcesCompat.getDrawable(resources, resource, null)!!
        val drawableColor = ResourcesCompat.getColor   (resources, R.color.colorHeaderImage, null)

        DrawableCompat.setTint(donationImage, drawableColor)
        nav_view.getHeaderView(0).donationImage.setImageDrawable(donationImage)
    }

    private fun setupActionBarDrawer() {
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun setupToolbar() {
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!billingManager.notifyActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
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
            id == R.id.menu_donate      -> showDonateDialog()
            id == R.id.menu_changelog   -> setCurrentFragment(AboutFragment())
            IS_IN_DEBUG_MODE            -> //Managing debug stuff here
                when (id) {
                    R.id.debug_menu_about -> {
                        Networker.obliterateCache()
                        Toast.makeText(this, "Cache obliterated! :)", Toast.LENGTH_SHORT).show()
                        switchToCalendarFragment()
                    }
                    R.id.debug_update_courses            -> {
                        LessonsUpdaterJob.runNowAndSchedulePeriodic()
                    }

                    R.id.debug_start_next_lesson_service -> {
                        NextLessonNotificationService.scheduleNow()
                    }

                    R.id.debug_reset_notification_tracker -> {
                        AppPreferences.notificationTracker.clear()
                        Toast.makeText(this, "Notification tracker reset!", Toast.LENGTH_SHORT).show()
                    }

                    R.id.debug_show_logs -> {
                        val logsString = HyperLog
                                .getDeviceLogs(false).orEmpty()
                                .asReversed()
                                .joinToString(separator = "\n") { it.deviceLog }

                        val logDialog = AlertDialog.Builder(this)
                        logDialog.setMessage(logsString)
                        logDialog.setPositiveButton("Copia") { _, _ ->
                            copyText(applicationContext, logsString)
                        }
                        logDialog.setNegativeButton("Cancella") { _, _ ->
                            HyperLog.getDeviceLogs(true)
                        }
                        logDialog.setCancelable(true)
                        logDialog.show()
                    }

                }
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showDonateDialog() {
        DonateDialog(this, billingManager).apply {
            onCloseAfterDonation.connect { productId ->
                val resourceHeader = findDonationItemByInternalId(productId).resourceHeader
                showAfterDonationAnimation(resourceHeader)
            }
            show()
        }
    }

    private fun showAfterDonationAnimation(@DrawableRes drawableId: Int) {
        drawer.post {
            //Opening the drawer
            drawer.openDrawer(Gravity.START)

            //Getting the image
            val donationImage = nav_view.getHeaderView(0).donationImage.apply {
                setImageDrawable(ResourcesCompat.getDrawable(resources, drawableId, null))
            }

            //Setting up the colors
            val colorBg    = ResourcesCompat.getColor(resources, R.color.backgroundMain, null)
            val colorImage = ResourcesCompat.getColor(resources, R.color.colorHeaderImage, null)

            //Setting up the animation
            val drawableToAnimate = donationImage.drawable.mutate()
            DrawableCompat.setTint(drawableToAnimate, colorBg)

            ValueAnimator.ofObject(ArgbEvaluator(), 0xFFFFFF, colorImage).apply {
                startDelay = 550 //waiting for the drawer to open
                duration = 1750   // milliseconds
                addUpdateListener {
                    DrawableCompat.setTint(drawableToAnimate, it.animatedValue as Int)
                }
                start()
            }
        }
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
