package com.geridea.trentastico.network.controllers


/*
 * Created with ♥ by Slava on 30/04/2017.
 */

import com.geridea.trentastico.database.Cacher
import com.geridea.trentastico.network.request.RequestSender

/**
 * The basic abstract class for all the executors.
 */
abstract class BasicController(
    /**
     * The request sender to used to send network request
     */
    val sender: RequestSender, val cacher: Cacher)
