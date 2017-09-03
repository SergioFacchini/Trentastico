package com.geridea.trentastico.network.controllers.listener

import com.geridea.trentastico.gui.views.requestloader.ILoadingMessage


/*
 * Created with ♥ by Slava on 01/09/2017.
 */

/**
 * [LessonsLoadingListener] used in situations when where we're interested only about whether the
 * loading operation was successful or not.
 */
abstract class LoadOrFailLessonsListener : LessonsLoadingListener {

    abstract fun onLoadingFailed()

    override fun onLoadingMessageDispatched(operation: ILoadingMessage) { ; }

    final override fun onNetworkErrorHappened(error: Exception, operationId: Int) {
        onLoadingFailed()
    }

    final override fun onParsingErrorHappened(exception: Exception, operationId: Int) {
        onLoadingFailed()
    }

    override fun onNothingFoundInCache() { ; }

    override fun onLoadingAborted(operationId: Int) { ; }


}