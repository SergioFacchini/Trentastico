package com.geridea.trentastico.gui.views.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout


/*
 * Created with ♥ by Slava on 19/08/2017.
 */

enum class LEPState(val viewIndex: kotlin.Int) { LOAD(0), ERROR(1), PRESENT(2) }

/**
 * Load, Error, Present view. View that has three different children and cycles between these as
 * requested.
 */
class LEPView : LinearLayout {

    var currentView = LEPState.LOAD
        set(value) {
            getChildAt(currentView.viewIndex).visibility = View.GONE
            getChildAt(value.viewIndex).visibility = View.VISIBLE

            field = value
        }

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    init {

    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount != 3) {
            throw IllegalStateException("The LEPView must have exactly three children!")
        }

        getChildAt(LEPState.LOAD   .viewIndex).visibility = GONE
        getChildAt(LEPState.ERROR  .viewIndex).visibility = GONE
        getChildAt(LEPState.PRESENT.viewIndex).visibility = GONE

        getChildAt(currentView.viewIndex).visibility = View.VISIBLE

        super.onLayout(changed, l, t, r, b)
    }

}