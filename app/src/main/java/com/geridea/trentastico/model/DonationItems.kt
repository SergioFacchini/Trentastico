package com.geridea.trentastico.model

import android.support.annotation.DrawableRes
import com.geridea.trentastico.R


/*
 * Created with ♥ by Slava on 24/10/2018.
 */
data class DonationItem(
        val internalId: Int,
        val sku: String,
        val description: String,
        @DrawableRes val resource: Int,
        @DrawableRes val resourceHeader: Int
)


val DONATION_ITEMS = listOf(
        DonationItem(
                10,
                "donation_bad_coffee",
                "Un caffè delle macchinette",
                R.drawable.ic_donation_bad_coffee,
                R.drawable.ic_donation_bad_coffee_white
        ),
        DonationItem(
                11,
                "donation_good_coffee",
                "Un caffè serio",
                R.drawable.ic_donation_good_coffee,
                R.drawable.ic_donation_good_coffee_white
        ),
        DonationItem(
                12,
                "donation_spritz",
                "Uno Spritz",
                R.drawable.ic_donation_spritz,
                R.drawable.ic_donation_spritz_white
        ),
        DonationItem(
                13,
                "donation_book",
                "Un libro",
                R.drawable.ic_donation_book,
                R.drawable.ic_donation_book_white
        )
)

