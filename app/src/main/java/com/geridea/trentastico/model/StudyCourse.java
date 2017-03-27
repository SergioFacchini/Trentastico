package com.geridea.trentastico.model;

import com.geridea.trentastico.providers.DepartmentsProvider;

import java.util.Locale;

public class StudyCourse {

    private long departmentId;
    private long courseId;
    private int year;

    public StudyCourse(long departmentId, long courseId, int year) {
        this.departmentId = departmentId;
        this.courseId = courseId;
        this.year = year;
    }

    public long getDepartmentId() {
        return departmentId;
    }

    public long getCourseId() {
        return courseId;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StudyCourse){
            StudyCourse course = (StudyCourse) obj;
            return course.departmentId == this.departmentId &&
                   course.courseId     == this.courseId &&
                   course.year         == this.year;
        }

        return false;
    }

    /**
     * Tries to decrease the year if possible; if not increases it by one. Used to get the previous
     * year's study course if possible.
     */
    public void decreaseOrChangeYear() {
        if (year == 1) {
            year++;
        } else {
            year--;
        }
    }

    public String generateFullDescription() {
        Department department = DepartmentsProvider.getDepartmentWithId(departmentId);
        Course course = department.getCourseWithId(courseId);

        return String.format(Locale.ITALY, "%s > %s - %d° anno",
                department.getName(), course.getName(), year
        );


    }
}
