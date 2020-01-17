package com.geridea.trentastico.gui.fragments

/*
 * Created with ♥ by Slava on 19/03/2017.
 */

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import com.alexvasilkov.android.commons.adapters.ItemsAdapter
import com.alexvasilkov.android.commons.utils.Views
import com.geridea.trentastico.R
import com.geridea.trentastico.gui.activities.FragmentWithMenuItems
import kotlinx.android.synthetic.main.fragment_other_apps.*
import android.content.Intent
import android.net.Uri


class OtherAppsFragment : FragmentWithMenuItems() {

    override val idsOfMenuItemsToMakeVisible: IntArray
        get() = IntArray(0)

    override fun bindMenuItem(item: MenuItem) = Unit

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
                inflater.inflate(R.layout.fragment_other_apps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding list view
        list_other_apps.adapter = buildAppAdapter()
        list_other_apps.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val app = list_other_apps.getItemAtPosition(position) as App

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(app.url))
            startActivity(browserIntent)
        }
    }

    private fun buildAppAdapter(): ItemsAdapter<App> {
        val adapter = object : ItemsAdapter<App>(this.context) {

            override fun createView(item: App, pos: Int, parent: ViewGroup?, inflater: LayoutInflater?): View =
                    inflater?.inflate(R.layout.itm_app, parent, false)!!

            override fun bindView(item: App, pos: Int, convertView: View?) {
                Views.find<TextView>(convertView, R.id.text_app_name).text = item.name
                Views.find<TextView>(convertView, R.id.text_app_description).text = item.description

                val appIcon = activity!!.resources.getDrawable(item.imageId)
                Views.find<ImageView>(convertView, R.id.image_app).setImageDrawable(appIcon)
            }
        }
        adapter.itemsList = listOf(
            App("RoomTick",
                imageId = R.drawable.app_roomtick,
                url = "https://roomtick.com/",
                description =
                "Risolve i problemi dei coinquilini fuori sede. " +
                "Traccia i debiti, ricorda i turni di pulizia, aggiorna la lista della spesa e molto altro."
            )
        )

        return adapter
    }
}

data class App(
    val name: String,
    val url: String,
    val description: String,
    @DrawableRes val imageId: Int
)

