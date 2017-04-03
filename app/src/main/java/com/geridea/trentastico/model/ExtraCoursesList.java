package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Iterator;

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

    public JSONArray toJSON() {
        JSONArray jsonArray = new JSONArray();

        for (ExtraCourse course: this) {
            jsonArray.put(course.toJSON());
        }

        return jsonArray;
    }

    public void removeHaving(long courseId, int year) {
        removeAll(getExtraCoursesHaving(courseId, year));
    }

    public ArrayList<ExtraCourse> getExtraCoursesHaving(long courseId, int year) {
        ArrayList<ExtraCourse> extraCourses = new ArrayList<>();
        for (ExtraCourse extraCourse : this) {
            if (extraCourse.getCourseId() == courseId || extraCourse.getYear() == year) {
                extraCourses.add(extraCourse);
            }
        }

        return extraCourses;
    }

    public void removeHavingLessonType(int lessonTypeId) {
        Iterator<ExtraCourse> iterator = this.iterator();
        while(iterator.hasNext()){
            ExtraCourse course = iterator.next();
            if (course.getLessonTypeId() == lessonTypeId) {
                iterator.remove();
            }
        }
    }

    public boolean hasCourseWithId(int lessonTypeId) {
        for (ExtraCourse extraCourse : this) {
            if (extraCourse.getLessonTypeId() == lessonTypeId) {
                return true;
            }
        }
        return false;
    }
}
