package com.geridea.trentastico.gui.views.requestloader;

import com.geridea.trentastico.model.ExtraCourse;
import com.geridea.trentastico.network.controllers.LessonsController;
import com.geridea.trentastico.utils.time.WeekInterval;

public class ExtraCoursesLoadingMessage extends AbstractTextMessage {

    private final WeekInterval interval;

    private final ExtraCourse course;
    private final boolean isARetry;

    public ExtraCoursesLoadingMessage(LessonsController.ExtraLessonsRequest request) {
        super(request.getOperationId());

        interval = request.getIntervalToLoad();

        this.course = request.getExtraCourse();
        this.isARetry = request.isRetrying();
    }


    @Override
    public String getText() {
        String format = isARetry ?
                 "Sto riprovando a scaricare gli orari del corso \"%s\" dal %s..."
               : "Sto scaricando gli orari del corso \"%s\" dal %s...";

        return String.format(format, course.getName(), formatFromToString(interval));
    }

}
