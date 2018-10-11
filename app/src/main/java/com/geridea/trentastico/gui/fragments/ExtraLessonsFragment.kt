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
import com.geridea.trentastico.gui.adapters.ExtraCoursesAdapter
import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.services.NextLessonNotificationService
import com.geridea.trentastico.utils.AppPreferences
import kotlinx.android.synthetic.main.fragment_extra_lessons.*


class ExtraLessonsFragment : FragmentWithMenuItems() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_extra_lessons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initLessonsList()

        lessonsList.setOnItemLongClickListener { _, _, position, _ ->
            val course = lessonsList.getItemAtPosition(position) as ExtraCourse

            val dialog = ExtraCourseDeleteDialog(requireContext(), course)
            dialog.onDeleteConfirm.connect {
                initLessonsList()

                //Updating notifications
                NextLessonNotificationService.removeNotificationsOfExtraCourse(requireContext(), course)
            }
            dialog.show()

            return@setOnItemLongClickListener true
        }

    }

    private fun initLessonsList() {
        val extraCourses = AppPreferences.extraCourses
        if (extraCourses.isEmpty()) {
            noExtraCoursesLabel.visibility = View.VISIBLE
        } else {
            noExtraCoursesLabel.visibility = View.GONE
        }

        lessonsList.adapter = ExtraCoursesAdapter(requireContext(), extraCourses)
    }


    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = intArrayOf(R.id.menu_add_extra_lessons)

    override fun bindMenuItem(item: MenuItem) {
        if (item.itemId == R.id.menu_add_extra_lessons) {
            item.setOnMenuItemClickListener {
                val dialog = ExtraCourseAddDialog(requireContext())
                dialog.onNewCourseAdded.connect {
                    initLessonsList()

                    //Updating notifications
                    NextLessonNotificationService.scheduleNow()
                }
                dialog.show()
                true
            }
        }
    }

}
