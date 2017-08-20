package com.geridea.trentastico.gui.views.requestloader

class LessonsLoadingMessage(operationId: Int, private val isARetry: Boolean):
        AbstractTextMessage(operationId) {

    override val text: String
        get() {
            return if (isARetry)
                "Sto riprovando a scaricare gli orari del tuo corso di studi..."
            else
                "Sto scaricando gli orari del tuo corso di studi..."
        }

}
