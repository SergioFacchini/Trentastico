package com.geridea.trentastico.model;

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

    public void addLessonSchedule(LessonSchedule lessonSchedule) {
        scheduledLessons.put(lessonSchedule.getId(), lessonSchedule);
    }

    public void mergeWith(LessonsSet lessons) {
        //When the filtering dialog is opened, the new, updated lessons set will not update the ones
        //that are already shown in the dialog. By doing the following we're ensuring that the
        //lesson types visibilities are consistent.
        for (LessonType lessonTypeToAdd : lessons.getLessonTypes()) {
            if (lessonTypes.containsKey(lessonTypeToAdd.getId())) {
                LessonType existingLessonType = lessonTypes.get(lessonTypeToAdd.getId());
                existingLessonType.setVisible(lessonTypeToAdd.isVisible());
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
}
