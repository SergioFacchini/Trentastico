package com.geridea.trentastico.model;

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
}
