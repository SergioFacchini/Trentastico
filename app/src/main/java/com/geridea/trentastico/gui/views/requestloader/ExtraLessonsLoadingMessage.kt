package com.geridea.trentastico.gui.views.requestloader

import com.geridea.trentastico.model.ExtraCourse

class ExtraLessonsLoadingMessage(
        private val extraCourse: ExtraCourse,
        operationId: Int,
        private val isARetry: Boolean = false) :
        AbstractTextMessage(operationId) {

    override val text: String
        get() = if (isARetry)
            "Sto riprovando a scaricare gli orari di \"${extraCourse.lessonName}\"..."
        else
            "Sto scaricando gli orari di \"${extraCourse.lessonName}\"..."

}