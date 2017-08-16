package com.geridea.trentastico.gui.views.requestloader

import com.geridea.trentastico.model.ExtraCourse
import com.geridea.trentastico.network.controllers.LessonsController
import com.geridea.trentastico.utils.time.WeekInterval

class ExtraCoursesLoadingMessage(request: LessonsController.ExtraLessonsRequest) : AbstractTextMessage(request.operationId) {

    private val interval: WeekInterval = request.intervalToLoad
    private val course: ExtraCourse    = request.extraCourse
    private val isARetry: Boolean      = request.isRetrying


    override val text: String
        get() {
            val format = if (isARetry)
                 "Sto riprovando a scaricare gli orari del corso \"%s\" dal %s..."
            else
                "Sto scaricando gli orari del corso \"%s\" dal %s..."

            return String.format(format, course.name, formatFromToString(interval))
        }

}
