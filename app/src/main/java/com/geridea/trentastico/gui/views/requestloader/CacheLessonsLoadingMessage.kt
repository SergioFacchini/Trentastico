package com.geridea.trentastico.gui.views.requestloader

class CacheLessonsLoadingMessage: AbstractTextMessage(nextAvailableId) {

    override val text: String
        get() = "Sto cercando gli orari del tuo corso di studi..."

    companion object {
        private var nextAvailableId = 0
            get() = field--
    }

}