package com.geridea.trentastico.model;

import java.util.ArrayList;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */
public class Department {
    private int id;
    private String name;

    private ArrayList<Course> courses = new ArrayList<>();

    public Department(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addCourse(Course course) {
        courses.add(course);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Course> getCourses() {
        return courses;
    }
}
