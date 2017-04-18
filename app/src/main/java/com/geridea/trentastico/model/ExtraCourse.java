package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 26/03/2017.
 */

import com.geridea.trentastico.logger.BugLogger;

import org.json.JSONException;
import org.json.JSONObject;

public class ExtraCourse {
    private int lessonTypeId;
    private long courseId;
    private int year;

    private String name;
    private String studyCourse;

    private int color;

    private ExtraCourse() { }

    public ExtraCourse(int lessonTypeId, long courseId, int year, String name, String studyCourse, int color) {
        this.lessonTypeId = lessonTypeId;
        this.courseId = courseId;
        this.year = year;
        this.name = name;
        this.studyCourse = studyCourse;
        this.color = color;
    }

    public ExtraCourse(StudyCourse studyCourse, LessonType lessonType) {
        this(
                lessonType.getId(),
                studyCourse.getCourseId(),
                studyCourse.getYear(),
                lessonType.getName(),
                studyCourse.generateFullDescription(),
                lessonType.getColor()
        );
    }

    public int getLessonTypeId() {
        return lessonTypeId;
    }

    public long getCourseId() {
        return courseId;
    }

    public int getYear() {
        return year;
    }

    public String getName() {
        return name;
    }

    public JSONObject toJSON(){
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("lessonTypeId", lessonTypeId);
            jsonObject.put("courseId", courseId);
            jsonObject.put("year", year);
            jsonObject.put("name", name);
            jsonObject.put("studyCourse", studyCourse);
            jsonObject.put("color", color);

            return jsonObject;
        } catch (JSONException e) {
            BugLogger.logBug("Converting extra course to JSON", e);
            throw new RuntimeException("Could not convert extra course to JSON");
        }
    }

    public static ExtraCourse fromJSON(JSONObject json){
        try {
            ExtraCourse course = new ExtraCourse();
            course.lessonTypeId = json.getInt("lessonTypeId");
            course.courseId     = json.getLong("courseId");
            course.year         = json.getInt("year");

            course.name         = json.getString("name");
            course.studyCourse  = json.getString("studyCourse");
            course.color        = json.getInt("color");

            return course;
        } catch (JSONException e) {
            BugLogger.logBug("Parsing extra course", e);
            throw new RuntimeException("Could not convert JSON to extra course");
        }

    }

    public int getColor() {
        return color;
    }

    public String getStudyCourseFullName() {
        return studyCourse;
    }

    public CourseAndYear getCourseAndYear() {
        CourseAndYear cay = new CourseAndYear();
        cay.courseId = getCourseId();
        cay.year     = getYear();
        return cay;
    }
}
