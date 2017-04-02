package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;

import java.util.ArrayList;

public class ExtraCoursesList extends ArrayList<ExtraCourse> {

    public ExtraCourse getCourseWithLessonType(long lessonTypeId) {
        for (ExtraCourse extraCourse : this) {
            if (extraCourse.getLessonTypeId() == lessonTypeId) {
                return extraCourse;
            }
        }

        BugLogger.logBug();
        throw new RuntimeException("Cannot find extra course with lesson type id: "+lessonTypeId);
    }

    public boolean isAnExtraLesson(LessonSchedule lesson) {
        for (ExtraCourse extraCourse : this) {
            if (extraCourse.getLessonTypeId() == lesson.getLessonTypeId()) {
                return true;
            }
        }

        return false;
    }
}
