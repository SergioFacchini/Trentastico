package com.geridea.trentastico.database


/*
 * Created with ♥ by Slava on 16/08/2017.
 */


//Cached periods
val CACHED_PERIOD_TABLE = "cached_periods"

val CP_ID           = "id"
val CP_START_WEEK   = "start_week"
val CP_START_YEAR   = "start_year"
val CP_END_WEEK     = "end_week"
val CP_END_YEAR     = "end_year"
val CP_PERIOD_TYPE  = "cached_period_type"
val CP_CACHED_IN_MS = "cached_in_ms"

internal val SQL_CREATE_CACHED_PERIOD =
     """CREATE TABLE $CACHED_PERIOD_TABLE (
         $CP_ID           INTEGER PRIMARY KEY AUTOINCREMENT,
         $CP_START_WEEK   INTEGER NOT NULL,
         $CP_START_YEAR   INTEGER NOT NULL,
         $CP_END_WEEK     INTEGER NOT NULL,
         $CP_END_YEAR     INTEGER NOT NULL,
         $CP_PERIOD_TYPE  INTEGER NOT NULL,
         $CP_CACHED_IN_MS INTEGER NOT NULL)"""


//Cached lessons
val CACHED_LESSONS_TABLE = "cached_lessons"

val CL_CACHED_PERIOD_ID = "cached_period_id"
val CL_LESSON_ID        = "lesson_id"
val CL_STARTS_AT_MS     = "starts_at_ms"
val CL_FINISHES_AT_MS   = "finishes_at_ms"
val CL_TEACHING_ID      = "teaching_id"
val CL_TEACHERS_NAMES   = "teachers_names"
val CL_WEEK_NUMBER      = "week_number"
val CL_YEAR             = "year"
val CL_SUBJECT          = "subject"
val CL_ROOM             = "room"
val CL_COLOR            = "color"

internal val SQL_CREATE_CACHED_LESSONS =
"""CREATE TABLE $CACHED_LESSONS_TABLE (
         $CL_CACHED_PERIOD_ID INTEGER NOT NULL,
         $CL_LESSON_ID        VARCHAR(100) NOT NULL,
         $CL_STARTS_AT_MS     INTEGER NOT NULL,
         $CL_FINISHES_AT_MS   INTEGER NOT NULL,
         $CL_TEACHING_ID      VARCHAR(100) NOT NULL,
         $CL_TEACHERS_NAMES   VARCHAR(200) NOT NULL,
         $CL_WEEK_NUMBER      INTEGER NOT NULL,
         $CL_YEAR             INTEGER NOT NULL,
         $CL_SUBJECT          VARCHAR(500) NOT NULL,
         $CL_ROOM             VARCHAR(500) NOT NULL,
         $CL_COLOR            INTEGER NOT NULL )"""


//Cached lesson types
val CACHED_LESSON_TYPES_TABLE = "cached_lesson_types"

val CLT_LESSON_TYPE_ID    = "lesson_type_id"
val CLT_NAME              = "name"
val CLT_PARTITIONING_NAME = "partitioning_name"
val CLT_COLOR             = "color"
val CLT_IS_EXTRA_COURSE   = "is_extra_course"

internal val SQL_CREATE_CACHED_LESSON_TYPES =
        """CREATE TABLE $CACHED_LESSON_TYPES_TABLE (
            $CLT_LESSON_TYPE_ID    VARCHAR(200) NOT NULL,
            $CLT_NAME              VARCHAR(500) NOT NULL,
            $CLT_PARTITIONING_NAME VARCHAR(500) NOT NULL,
            $CLT_COLOR             INTEGER NOT NULL,
            $CLT_IS_EXTRA_COURSE   INTEGER NOT NULL)"""



//Library opening times
val CACHED_LIBRARY_TIMES_TABLE_NAME = "cached_library_times"

val CLIBT_DAY          = "day"
val CLIBT_BUC          = "buc"
val CLIBT_CIAL         = "cial"
val CLIBT_MESIANO      = "mesiano"
val CLIBT_POVO         = "povo"
val CLIBT_PSICOLOGIA   = "psicologia"
val CLIBT_CACHED_IN_MS = "cached_in_ms"

internal val SQL_CREATE_CACHED_LIBRARY_TIMES =
    """CREATE TABLE $CACHED_LIBRARY_TIMES_TABLE_NAME (
        $CLIBT_DAY          CHAR(10) NOT NULL PRIMARY KEY,
        $CLIBT_BUC          VARCHAR(10) NOT NULL,
        $CLIBT_CIAL         VARCHAR(10) NOT NULL,
        $CLIBT_MESIANO      VARCHAR(10) NOT NULL,
        $CLIBT_POVO         VARCHAR(10) NOT NULL,
        $CLIBT_PSICOLOGIA   VARCHAR(10) NOT NULL,
        $CLIBT_CACHED_IN_MS INT NOT NULL )"""
