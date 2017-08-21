package com.geridea.trentastico.utils

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase


/*
 * Created with ♥ by Slava on 21/08/2017.
 */

/**
 * @see SQLiteDatabase.query
 */
fun SQLiteDatabase.query(tableName: String, columns: Array<String>): Cursor =
        query(tableName, columns, null, null, null, null, null)

/**
 * @see SQLiteDatabase.query
 */
fun SQLiteDatabase.query(tableName: String, columns: Array<String>, query: String): Cursor =
        query(tableName, columns, query, arrayOf(), null, null, null)

/**
 * @see SQLiteDatabase.query
 */
fun SQLiteDatabase.query( tableName: String, columns: Array<String>, query: String, values: Array<String>): Cursor =
        query(tableName, columns, query, values, null, null, null)




fun Cursor.getString(columnName: String): String
        = getString(getColumnIndexOrThrow(columnName))

fun Cursor.getNullableString(columnName: String): String?
        = getString(getColumnIndexOrThrow(columnName))

fun Cursor.getInt(columnName: String): Int
        = getInt(getColumnIndexOrThrow(columnName))

fun Cursor.getLong(columnName: String): Long
        = getLong(getColumnIndexOrThrow(columnName))