package com.geridea.trentastico.database;


/*
 * Created with ♥ by Slava on 03/04/2017.
 */

import com.geridea.trentastico.model.cache.CachedLessonsSet;

public interface LessonsSetAvailableListener {

    void onLessonsSetAvailable(CachedLessonsSet lessonsSet);

}
