package com.geridea.trentastico.gui.fragments


/*
 * Created with ♥ by Slava on 22/04/2017.
 */

import android.content.Context
import com.geridea.trentastico.R
import com.github.porokoro.paperboy.ItemType
import com.github.porokoro.paperboy.PaperboyFragment
import com.github.porokoro.paperboy.ViewTypes
import com.github.porokoro.paperboy.builders.buildItemType
import com.github.porokoro.paperboy.builders.buildPaperboy
import java.util.*

object PaperboyFragmentMaker {

    fun buildPaperboyFragment(context: Context): PaperboyFragment = buildPaperboy(context, {
        val featureTypes = ArrayList<ItemType>()

        featureTypes.add(buildItemType(context, 3, "Ottimo!", "3", {
            colorRes      = R.color.changelog_improvement
            titleSingular = "Miglioramento"
            titlePlural   = "Miglioramenti"
            icon          = R.drawable.ic_sentiment_satisfied_black_24dp
            sortOrder     = 3
        }))

        featureTypes.add(buildItemType(context, 2, "Super", "2", {
            colorRes      = R.color.changelog_bug_fixed
            titleSingular = "Correzione"
            titlePlural   = "Correzioni"
            icon          = R.drawable.ic_mood_black_24dp
            sortOrder     = 2
        }))

        featureTypes.add(buildItemType(context, 1, "Wow", "1", {
            colorRes      = R.color.changelog_new_feature
            titleSingular = "Novità"
            titlePlural   = "Novità"
            icon          = R.drawable.ic_sentiment_very_satisfied_black_24dp
            sortOrder     = 1
        }))


        viewType      = ViewTypes.ICON
        sectionLayout = R.layout.view_paperboy_version
        itemLayout    = R.layout.view_paperboy_item
        sortItems     = true
        itemTypes     = featureTypes
    })
}


