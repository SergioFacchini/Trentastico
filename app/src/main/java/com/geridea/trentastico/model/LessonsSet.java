package com.geridea.trentastico.model;

import com.geridea.trentastico.logger.BugLogger;
import com.geridea.trentastico.utils.AppPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/*
 * Created with ♥ by Slava on 12/03/2017.
 */
public class LessonsSet {

    protected final HashMap<Long, LessonSchedule> scheduledLessons;
    protected final HashMap<Integer, LessonType> lessonTypes;

    public LessonsSet() {
        lessonTypes      = new HashMap<>();
        scheduledLessons = new HashMap<>();
    }

    public Collection<LessonType> getLessonTypes() {
        return lessonTypes.values();
    }

    public Collection<LessonSchedule> getScheduledLessons() {
        return scheduledLessons.values();
    }

    public void addLessonType(LessonType lessonType) {
        lessonTypes.put(lessonType.getId(), lessonType);
    }

    public void mergeWith(LessonsSet lessons) {
        //When the filtering dialog is opened, the new, updated lessons set will not update the ones
        //that are already shown in the dialog. By doing the following we're ensuring that the
        //lesson types visibilities are consistent.
        for (LessonType lessonTypeToAdd: lessons.getLessonTypes()) {
            if (lessonTypes.containsKey(lessonTypeToAdd.getId())) {
                LessonType existingLessonType = lessonTypes.get(lessonTypeToAdd.getId());
                existingLessonType.setVisible(lessonTypeToAdd.isVisible());
                existingLessonType.mergePartitionings(lessonTypeToAdd.getPartitioning());
            } else {
                lessonTypes.put(lessonTypeToAdd.getId(), lessonTypeToAdd);
            }
        }

        for (LessonSchedule lessonSchedule : lessons.getScheduledLessons()) {
            scheduledLessons.put(lessonSchedule.getId(), lessonSchedule);
        }
    }

    public void recalculatePartitionings() {
        for (LessonType lessonType : getLessonTypes()) {

            Partitioning previous = Partitioning.NONE;
            for (LessonSchedule lesson : LessonSchedule.getLessonsOfType(lessonType, this.getScheduledLessons())) {

                Partitioning found = LessonType.findPartitioningFromDescription(lesson.getFullDescription());
                if (found.getType() != PartitioningType.NONE) {
                    if(previous.getType() == PartitioningType.NONE){
                        //We found the first partitioning
                        previous = found;
                    } else if(found.getType().equals(previous.getType())){
                        previous.mergePartitionCases(found);
                    } else {
                        //We found two lessons with different partitioning methods. We just ignore this
                        //situation for now and consider it to be not partitioned.
                        BugLogger.logBug();
                        previous = Partitioning.NONE;
                        break;
                    }
                }
            }

            previous.hidePartitioningsInList(AppPreferences.getHiddenPartitionings(lessonType.getId()));
            lessonType.setPartitioning(previous);
        }

    }

    public void addLessonSchedules(ArrayList<LessonSchedule> lessons) {
        for (LessonSchedule lesson : lessons) {
            scheduledLessons.put(lesson.getId(), lesson);
        }

        recalculatePartitionings();
    }

    public void removeLessonTypesNotInCurrentSemester() {
        Iterator<LessonType> typesIterator = getLessonTypes().iterator();

        while(typesIterator.hasNext()){
            LessonType lessonType = typesIterator.next();

            boolean keepLessonType = false;
            ArrayList<LessonSchedule> lessons = LessonSchedule.getLessonsOfType(lessonType, this.getScheduledLessons());

            for (LessonSchedule lesson : lessons) {
                if (Semester.isInCurrentSemester(lesson)) {
                    keepLessonType = true;
                    break;
                }
            }

            if (!keepLessonType) {
                typesIterator.remove();
            }

        }

    }

    /**
     * Removes all lessons and lesson types types not matching the lesson type passed in the
     * parameter.
     */
    public void prepareForExtraCourse(ExtraCourse extraCourse) {
        Iterator<LessonType> lessonTypes = getLessonTypes().iterator();
        while (lessonTypes.hasNext()){
            LessonType lessonType = lessonTypes.next();

            if (lessonType.getId() != extraCourse.getLessonTypeId()) {
                lessonTypes.remove();
            }
        }

        Iterator<LessonSchedule> scheduledLessons = getScheduledLessons().iterator();
        while(scheduledLessons.hasNext()){
            LessonSchedule lesson = scheduledLessons.next();
            if (lesson.getLessonTypeId() != extraCourse.getLessonTypeId()) {
                scheduledLessons.remove();
            }
        }
    }

    public void filterLessons() {
        LessonSchedule.filterLessons(this.scheduledLessons.values());
    }

}
