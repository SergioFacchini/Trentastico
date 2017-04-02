package com.geridea.trentastico.gui.adapters;


/*
 * Created with ♥ by Slava on 01/04/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.network.request.LessonsDiffResult;
import com.geridea.trentastico.utils.StringUtils;
import com.geridea.trentastico.utils.time.CalendarUtils;

public abstract class DiffResultItem {

    private final LessonSchedule lesson;

    public DiffResultItem(LessonSchedule lesson) {
        this.lesson = lesson;
    }

    public String getCourseName() {
        return lesson.getSubject();
    }

    public String getScheduledDay() {
        return CalendarUtils.formatEEEEDDMMMM(lesson.getStartsAt());
    }

    public String getScheduledHours() {
        return CalendarUtils.formatHHMM(lesson.getStartsAt());
    }

    public int getDuration() {
        return lesson.getDurationInMinutes();
    }

    public int getColor() {
        return lesson.getColor();
    }

    public abstract String getDiffDescription();

    public abstract boolean hasModifications();

    public abstract String getModifications();

    public static DiffResultItem buildAdded(LessonSchedule lesson) {
        return new AddDiffResult(lesson);
    }

    public static DiffResultItem buildRemoved(LessonSchedule lesson) {
        return new RemoveDiffResult(lesson);
    }

    public static DiffResultItem buildChanged(LessonsDiffResult.LessonChange change) {
        return new ChangeDiffResult(change);
    }

    private static class AddDiffResult extends DiffResultItem {

        public AddDiffResult(LessonSchedule lesson) {
            super(lesson);
        }

        @Override
        public String getDiffDescription() {
            return "È stata aggiunta questa lezione:";
        }

        @Override
        public boolean hasModifications() {
            return false;
        }

        @Override
        public String getModifications() {
            return null;
        }
    }

    private static class RemoveDiffResult extends DiffResultItem {

        public RemoveDiffResult(LessonSchedule lesson) {
            super(lesson);
        }

        @Override
        public String getDiffDescription() {
            return "Questa lezione è stata annullata:";
        }

        @Override
        public boolean hasModifications() {
            return false;
        }

        @Override
        public String getModifications() {
            return null;
        }
    }

    private static class ChangeDiffResult extends DiffResultItem {

        private LessonsDiffResult.LessonChange lessonChange;

        public ChangeDiffResult(LessonsDiffResult.LessonChange lessonChange) {
            super(lessonChange.getOriginal());

            this.lessonChange = lessonChange;
        }

        @Override
        public String getDiffDescription() {
            return "La lezione ha subito variazioni:";
        }

        @Override
        public boolean hasModifications() {
            return true;
        }

        @Override
        public String getModifications() {
            return StringUtils.implode(lessonChange.getDifferences(), "\n");
        }
    }


}
