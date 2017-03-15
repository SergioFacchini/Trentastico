package com.geridea.trentastico.model;

public class StudyCourse {

    private long department;
    private long course;
    private long year;

    public StudyCourse(long department, long course, long year) {
        this.department = department;
        this.course = course;
        this.year = year;
    }

    public long getDepartmentId() {
        return department;
    }

    public long getCourseId() {
        return course;
    }

    public long getYear() {
        return year;
    }
}
