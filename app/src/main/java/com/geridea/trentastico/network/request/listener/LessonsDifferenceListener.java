package com.geridea.trentastico.network.request.listener;


/*
 * Created with ♥ by Slava on 31/03/2017.
 */

import com.geridea.trentastico.network.request.LessonsDiffResult;

public interface LessonsDifferenceListener {

    void onLoadingError();

    void onRequestCompleted();

    void onNumberOfRequestToSendKnown(int numRequests);

    void onNoLessonsInCache();

    void onDiffResult(LessonsDiffResult lessonsDiffResult);
}
