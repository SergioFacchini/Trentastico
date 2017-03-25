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

    /**
     * @return the first lesson having a given lesson type.
     */
    public LessonSchedule getALessonHavingType(LessonType lessonType) {
        for (LessonSchedule scheduledLesson : scheduledLessons.values()) {
            if(scheduledLesson.hasLessonType(lessonType)){
                return scheduledLesson;
            }
        }

        //Technically, should never happen
        return null;
    }

    public void removeLessonsWithTypeIds(ArrayList<Integer> lessonTypesIdsToHide) {
        Iterator<LessonSchedule> lessonsIterator = scheduledLessons.values().iterator();

        while(lessonsIterator.hasNext()){
            LessonSchedule schedule = lessonsIterator.next();
            for (Integer lessonTypeIdToHide : lessonTypesIdsToHide) {
                if (schedule.getLessonTypeId() == lessonTypeIdToHide) {
                    lessonsIterator.remove();
                    break;
                }
            }
        }
    }

    public void recalculatePartitionings() {
        for (LessonType lessonType : getLessonTypes()) {

            Partitioning previous = Partitioning.NONE;
            for (LessonSchedule lesson : getLessonsOfType(lessonType)) {

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

    public ArrayList<LessonSchedule> getLessonsOfType(LessonType lessonType) {
        ArrayList<LessonSchedule> toReturn = new ArrayList<>();

        for (LessonSchedule lessonSchedule : getScheduledLessons()) {
            if (lessonSchedule.getLessonTypeId() == lessonType.getId()) {
                toReturn.add(lessonSchedule);
            }
        }

        return toReturn;
    }

    public void addLessonSchedules(ArrayList<LessonSchedule> lessons) {
        for (LessonSchedule lesson : lessons) {
            scheduledLessons.put(lesson.getId(), lesson);
        }

        recalculatePartitionings();
    }

    public void removeLessonsWithHiddenPartitionings() {
        ArrayList<LessonSchedule> lessonsToRemove = new ArrayList<>();

        for (LessonType lessonType : getLessonTypes()) {
            ArrayList<LessonSchedule> lessons = getLessonsOfType(lessonType);
            PartitioningType partitioningType = lessonType.getPartitioning().getType();

            if (partitioningType != PartitioningType.NONE) {
                ArrayList<PartitioningCase> partitioningsToHide = lessonType.findPartitioningsToHide();
                for (LessonSchedule lesson : lessons) {
                    if (!lesson.matchesPartitioningType(partitioningType)) {
                        //Technically this should never happen since the partitioning type is calculated
                        //from lessons; however it might be possible that partitionings are introduced
                        //after the course start. I want to track this possible issue to check whenever
                        //this might happen.
                        BugLogger.logBug();
                    } else if (lesson.matchesAnyOfPartitioningCases(partitioningsToHide)) {
                        lessonsToRemove.add(lesson);
                    }
                }
            }

        }

        scheduledLessons.values().removeAll(lessonsToRemove);
    }
}
