package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 13/04/2017.
 */

import com.geridea.trentastico.model.CourseAndYear;
import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.model.LessonsSet;
import com.geridea.trentastico.network.request.listener.LessonWithRoomFetchedListener;
import com.geridea.trentastico.utils.time.CalendarInterval;

import org.json.JSONException;

import java.util.Calendar;

public class FetchRoomForLessonRequest extends BasicLessonsRequest {

    private final LessonSchedule lesson;
    private final CourseAndYear cay;
    private final LessonWithRoomFetchedListener listener;

    public FetchRoomForLessonRequest(LessonSchedule lesson, CourseAndYear cay, LessonWithRoomFetchedListener listener) {
        this.lesson = lesson;
        this.cay = cay;
        this.listener = listener;

    }

    @Override
    public void notifyFailure(Exception e, RequestSender sender) {
        //In this request we just don't manage errors
        listener.onLessonUpdated(lesson);
    }

    @Override
    public void manageResponse(String response, RequestSender sender) {
        try {
            LessonsSet lessonsSet = parseResponse(response);
            for (LessonSchedule fetchedLesson : lessonsSet.getScheduledLessons()) {
                if (fetchedLesson.getId() == lesson.getId()) {
                    lesson.setRoom(fetchedLesson.getRoom());
                    break;
                }
            }

            //Note: here might happen that the lesson we were trying to fetch the room for is not
            //available; actually this happens were rarely, when we try to get the lesson's room
            //but exactly at that time the lessons gets removed. In this case we just keep the
            //lesson as it is, even though the best way to handle this would be to not consider that
            //lesson for further elaborations.
            listener.onLessonUpdated(lesson);
        } catch (JSONException e) {
            notifyFailure(e, sender);
        }

    }

    @Override
    public void notifyResponseUnsuccessful(int code, RequestSender sender) {
        //In this request we just don't manage errors
        listener.onLessonUpdated(lesson);
    }

    @Override
    public void notifyOnBeforeSend() { }

    @Override
    protected CalendarInterval getCalendarIntervalToLoad() {
        //For some reasons, trying to fetch too small intervals does not returns us any result!
        return lesson.toExpandedCalendarInterval(Calendar.HOUR_OF_DAY, 4);
    }

    @Override
    protected long getCourseId() {
        return cay.courseId;
    }

    @Override
    protected int getYear() {
        return cay.year;
    }

}
