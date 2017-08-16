package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TextView

import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LibraryOpeningTimes
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.LibraryOpeningTimesListener
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils

import java.text.SimpleDateFormat
import java.util.Calendar

import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick

class LibrariesFragment : FragmentWithMenuItems(), LibraryOpeningTimesListener {

    private enum class State {
        LOADING, SHOWING, ERROR
    }

    @BindView(R.id.loader_spinner)   internal var spinner: ProgressBar? = null
    @BindView(R.id.timetables)       internal var timetables: TableLayout? = null
    @BindView(R.id.error_panel)      internal var errorPanel: View? = null

    @BindView(R.id.current_day_name) internal var currentDayName: TextView? = null

    @BindView(R.id.buc_times)        internal lateinit var timesBuc: TextView
    @BindView(R.id.cial_times)       internal lateinit var timesCial: TextView
    @BindView(R.id.mesiano_times)    internal lateinit var timesMesiano: TextView
    @BindView(R.id.povo_times)       internal lateinit var timesPovo: TextView
    @BindView(R.id.psicologia_times) internal lateinit var timesPsicologia: TextView

    private var currentDay: Calendar = CalendarUtils.debuggableToday

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_libraries, container, false)
        ButterKnife.bind(this, view)

        currentDay = CalendarUtils.debuggableToday

        showCurrentDay(currentDay)
        loadOpeningTimesForDate(currentDay)

        return view
    }

    private fun showCurrentDay(currentDay: Calendar) {
        currentDayName!!.text = interpretDay(currentDay)
    }

    private fun interpretDay(currentDay: Calendar): String {
        if (CalendarUtils.isSameDay(currentDay, CalendarUtils.debuggableToday)) {
            return String.format("Oggi (%s)", DAY_NAME.format(currentDay.time))
        }

        val tomorrow = CalendarUtils.debuggableToday
        tomorrow.add(Calendar.DAY_OF_MONTH, +1)
        if (CalendarUtils.isSameDay(currentDay, tomorrow)) {
            return String.format("Domani (%s)", DAY_NAME.format(currentDay.time))
        }

        val yesterday = CalendarUtils.debuggableToday
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        return if (CalendarUtils.isSameDay(currentDay, yesterday)) {
            String.format("Ieri (%s)", DAY_NAME.format(currentDay.time))
        } else CURRENT_DAY_FORMAT.format(currentDay.time)

    }

    private fun loadOpeningTimesForDate(day: Calendar?) {
        showState(State.LOADING)

        Networker.getLibraryOpeningTimes(day!!.clone() as Calendar, this)
    }

    private fun showState(state: State) {
        UIUtils.runOnMainThread {
            spinner!!.visibility = View.GONE
            timetables!!.visibility = View.GONE
            errorPanel!!.visibility = View.GONE

            when (state) {
                LibrariesFragment.State.SHOWING -> timetables!!.visibility = View.VISIBLE
                LibrariesFragment.State.ERROR -> errorPanel!!.visibility = View.VISIBLE
                LibrariesFragment.State.LOADING -> spinner!!.visibility = View.VISIBLE
            }
        }
    }

    @OnClick(R.id.previous_day)
    internal fun onPreviousDayButtonClicked() {
        currentDay.add(Calendar.DAY_OF_MONTH, -1)

        showCurrentDay(currentDay)
        loadOpeningTimesForDate(currentDay)
    }

    @OnClick(R.id.next_day)
    internal fun onNextDayButtonClicked() {
        currentDay.add(Calendar.DAY_OF_MONTH, +1)

        showCurrentDay(currentDay)
        loadOpeningTimesForDate(currentDay)
    }

    @OnClick(R.id.retry_fetch_times)
    internal fun onRetryFetchTimesButtonClicked() {
        loadOpeningTimesForDate(currentDay)
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) {
        //Nothing to bind
    }

    override fun onOpeningTimesLoaded(times: LibraryOpeningTimes, date: Calendar) {
        //We can start loading the current day, then switch to the next day.
        //In this case, we don't want to mess up with dates
        if (currentDay.timeInMillis == date.timeInMillis) {
            showTimes(times)
        }

    }

    private fun showTimes(times: LibraryOpeningTimes) {
        UIUtils.runOnMainThread {
            timesBuc.text        = times.timesBuc
            timesCial.text       = times.timesCial
            timesMesiano.text    = times.timesMesiano
            timesPovo.text       = times.timesPovo
            timesPsicologia.text = times.timesPsicologia

            showState(State.SHOWING)
        }
    }

    override fun onOpeningTimesLoadingError() {
        showState(State.ERROR)
    }

    override fun onErrorParsingResponse(e: Exception) {
        BugLogger.logBug("ERROR PARSING LIBRARY OPENING TIMES", e)
        showState(State.ERROR)
    }

    companion object {
        private val CURRENT_DAY_FORMAT = SimpleDateFormat("EEEE dd MMM")
        private val DAY_NAME           = SimpleDateFormat("EEEE")
    }

}
