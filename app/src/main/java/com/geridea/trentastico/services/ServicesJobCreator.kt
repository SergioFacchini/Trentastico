package com.geridea.trentastico.services

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator


/*
 * Created with ♥ by Slava on 10/10/2018.
 */

class ServicesJobCreator : JobCreator {

    override fun create(tag: String): Job? {

        return when (tag) {
            LessonsUpdaterJob.TAG             -> LessonsUpdaterJob()
            NextLessonNotificationService.TAG -> NextLessonNotificationService()
            else -> null
        }
    }

}