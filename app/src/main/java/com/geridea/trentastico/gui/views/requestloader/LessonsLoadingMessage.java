package com.geridea.trentastico.gui.views.requestloader;

import com.geridea.trentastico.utils.time.WeekInterval;

public class LessonsLoadingMessage extends AbstractTextMessage {

    private final boolean isARetry;

    private WeekInterval intervalToLoad;

    public LessonsLoadingMessage(int operationId, WeekInterval intervalToLoad, boolean isARetry) {
        super(operationId);

        this.intervalToLoad = intervalToLoad;
        this.isARetry = isARetry;
    }

    @Override
    public String getText() {
        String fromTo = formatFromToString(intervalToLoad);

        return isARetry ?
                "Sto riprovando a scaricare gli orari dal "+fromTo+"...":
                "Sto scaricando gli orari dal "+fromTo+"...";
    }

}
