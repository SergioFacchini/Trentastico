package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.model.LessonSchedule;
import com.geridea.trentastico.utils.time.CalendarUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static com.geridea.trentastico.utils.time.CalendarUtils.getDebuggableMillis;

public class LessonsDiffResult implements Serializable {

    private final ArrayList<LessonSchedule> removedLessons = new ArrayList<>();
    private final ArrayList<LessonSchedule> addedLessons   = new ArrayList<>();

    private final ArrayList<LessonChange> changedLessons = new ArrayList<>();

    public void addRemovedLesson(LessonSchedule lesson) {
        removedLessons.add(lesson);
    }

    public void addAddedLesson(LessonSchedule lesson) {
        addedLessons.add(lesson);
    }

    public void addChangedLesson(LessonSchedule original, LessonSchedule changed) {
        changedLessons.add(new LessonChange(original, changed));
    }

    public ArrayList<LessonSchedule> getRemovedLessons() {
        return removedLessons;
    }

    public ArrayList<LessonSchedule> getAddedLessons() {
        return addedLessons;
    }

    public ArrayList<LessonChange> getChangedLessons() {
        return changedLessons;
    }

    public void addFrom(LessonsDiffResult anotherDiff) {
        addedLessons.addAll(anotherDiff.addedLessons);
        removedLessons.addAll(anotherDiff.removedLessons);
        changedLessons.addAll(anotherDiff.changedLessons);
    }

    public boolean isEmpty() {
        return addedLessons.isEmpty() && removedLessons.isEmpty() && changedLessons.isEmpty();
    }

    public int getNumTotalDifferences() {
        return addedLessons.size() + removedLessons.size() + changedLessons.size();
    }

    public void discardPastLessons() {
        discardPastLessons(addedLessons);
        discardPastLessons(removedLessons);
        discardPastLessonsChanges(changedLessons);
    }

    private void discardPastLessonsChanges(ArrayList<LessonChange> lessons) {
        long currentMillis = getDebuggableMillis();

        Iterator<LessonChange> iterator = lessons.iterator();
        while(iterator.hasNext()) {
            LessonChange lesson = iterator.next();
            if(lesson.getOriginal().startsBefore(currentMillis)){
                iterator.remove();
            }
        }
    }

    private void discardPastLessons(ArrayList<LessonSchedule> lessons) {
        long currentMillis = getDebuggableMillis();

        Iterator<LessonSchedule> iterator = lessons.iterator();
        while(iterator.hasNext()) {
            LessonSchedule lesson = iterator.next();
            if(lesson.startsBefore(currentMillis)){
                iterator.remove();
            }
        }
    }

    public class LessonChange implements Serializable {

        private final LessonSchedule original;
        private final LessonSchedule changed;

        public LessonChange(LessonSchedule original, LessonSchedule changed) {
            this.original = original;
            this.changed = changed;
        }

        public LessonSchedule getOriginal() {
            return original;
        }

        public LessonSchedule getChanged() {
            return changed;
        }

        public Collection getDifferences() {
            ArrayList<String> differences = new ArrayList<>(3);
            boolean descriptionDetailsChanged = false;

            if (original.getStartsAt() != changed.getStartsAt()) {
                String day = CalendarUtils.formatEEEEDDMMMM(changed.getStartsAt());
                String hours = CalendarUtils.formatHHMM(changed.getStartsAt());
                differences.add(String.format("Lezione spostata a %s alle ore %s", day, hours));
            }

            if (original.getDurationInMinutes() != changed.getDurationInMinutes()) {
                differences.add(
                        String.format("La lezione durerà %d min invece di %d.",
                                changed.getDurationInMinutes(),
                                original.getDurationInMinutes()
                        )
                );
            }

            if (!original.getRoom().equals(changed.getRoom())) {
                differences.add(
                    "La lezione si terrà in \"" + changed.getRoom()+"\" invece di \""+original.getRoom()+"\""
                );
                descriptionDetailsChanged = true;
            }

            if (!original.getSubject().equals(changed.getSubject())) {
                differences.add(
                        String.format("Il corso della lezione è stato modificato in \"%s\".", changed.getSubject())
                );
                descriptionDetailsChanged = true;
            }

            if (!descriptionDetailsChanged && !original.getFullDescription().equals(changed.getFullDescription())) {
                differences.add(
                        "I dettagli della lezione sono cambiati:-------------\n " + changed.getFullDescription()
                );
            }

            return differences;
        }
    }
}
