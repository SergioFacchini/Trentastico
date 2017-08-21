package com.geridea.trentastico.model


/*
 * Created with ♥ by Slava on 20/08/2017.
 */
data class LessonTypeNew(
        val id: String,
        val name: String,
        val teachersNames: String,
        val color: Int,
        var isVisible: Boolean = true) {

    override fun equals(other: Any?): Boolean =
            //There were some problems with original equals due to the fact that lesson types might
            //be set visible or not. This caused one visible and one hidden lesson type to be
            //considered different
            if (other is LessonTypeNew) other.id == this.id else false

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + teachersNames.hashCode()
        result = 31 * result + color
        return result
    }
}