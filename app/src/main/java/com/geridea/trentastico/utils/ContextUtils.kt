package com.geridea.trentastico.utils


/*
 * Created with ♥ by Slava on 30/03/2017.
 */

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

object ContextUtils {

    fun weHaveInternet(context: Context): Boolean {
        var haveConnectedWifi = false
        var haveConnectedMobile = false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (ni in cm.allNetworkInfo) {
            if (ni.typeName.equals("WIFI", ignoreCase = true))
                if (ni.isConnected)
                    haveConnectedWifi = true
            if (ni.typeName.equals("MOBILE", ignoreCase = true))
                if (ni.isConnected)
                    haveConnectedMobile = true
        }
        return haveConnectedWifi || haveConnectedMobile
    }
}
