package com.geridea.trentastico.gui.views.requestloader

import com.geridea.trentastico.utils.time.WeekInterval

class LessonsLoadingMessage(operationId: Int, private val intervalToLoad: WeekInterval, private val isARetry: Boolean) : AbstractTextMessage(operationId) {

    override val text: String
        get() {
            val fromTo = formatFromToString(intervalToLoad)

            return if (isARetry)
                "Sto riprovando a scaricare gli orari dal $fromTo..."
            else
                "Sto scaricando gli orari dal $fromTo..."
        }

}
