package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import com.geridea.trentastico.logger.BugLogger
import com.geridea.trentastico.model.LibraryOpeningTimes
import com.geridea.trentastico.network.Networker
import com.geridea.trentastico.network.controllers.listener.LibraryOpeningTimesListener
import com.geridea.trentastico.utils.UIUtils
import com.geridea.trentastico.utils.time.CalendarUtils
import kotlinx.android.synthetic.main.fragment_libraries.*
import java.text.SimpleDateFormat
import java.util.*

class LibrariesFragment : FragmentWithMenuItems(), LibraryOpeningTimesListener {

    private enum class State { LOADING, SHOWING, ERROR }

    private var currentDay: Calendar = CalendarUtils.debuggableToday

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_libraries, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentDay = CalendarUtils.debuggableToday

        showCurrentDay(currentDay)
        loadOpeningTimesForDate(currentDay)



        previousDay.setOnClickListener {
            currentDay.add(Calendar.DAY_OF_MONTH, -1)

            showCurrentDay(currentDay)
            loadOpeningTimesForDate(currentDay)
        }

        next_day.setOnClickListener {
            currentDay.add(Calendar.DAY_OF_MONTH, +1)

            showCurrentDay(currentDay)
            loadOpeningTimesForDate(currentDay)
        }

        retry_fetch_times.setOnClickListener {
            loadOpeningTimesForDate(currentDay)
        }


        super.onViewCreated(view, savedInstanceState)
    }

    private fun showCurrentDay(currentDay: Calendar) {
        currentDayName.text = interpretDay(currentDay)
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

    private fun showState(state: State) = UIUtils.runOnMainThread {
        loaderSpinner.visibility = View.GONE
        timetables.visibility = View.GONE
        errorPanel.visibility = View.GONE

        when (state) {
            LibrariesFragment.State.SHOWING -> timetables.visibility = View.VISIBLE
            LibrariesFragment.State.ERROR   -> errorPanel.visibility = View.VISIBLE
            LibrariesFragment.State.LOADING -> loaderSpinner.visibility = View.VISIBLE
        }
    }

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = //Nothing to bind
            Unit

    override fun onOpeningTimesLoaded(times: LibraryOpeningTimes, date: Calendar) {
        //We can start loading the current day, then switch to the next day.
        //In this case, we don't want to mess up with dates
        if (currentDay.timeInMillis == date.timeInMillis) {
            showTimes(times)
        }

    }

    private fun showTimes(times: LibraryOpeningTimes) = UIUtils.runOnMainThread {
        timesBuc.text        = times.timesBuc
        timesCial.text       = times.timesCial
        timesMesiano.text    = times.timesMesiano
        timesPovo.text       = times.timesPovo
        timesPsicologia.text = times.timesPsicologia

        showState(State.SHOWING)
    }

    override fun onOpeningTimesLoadingError() = showState(State.ERROR)

    override fun onErrorParsingResponse(e: Exception) {
        BugLogger.logBug("ERROR PARSING LIBRARY OPENING TIMES", e)
        showState(State.ERROR)
    }

    companion object {
        private val CURRENT_DAY_FORMAT = SimpleDateFormat("EEEE dd MMM")
        private val DAY_NAME           = SimpleDateFormat("EEEE")
    }

}
