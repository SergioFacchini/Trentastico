package com.geridea.trentastico.model

import com.geridea.trentastico.model.cache.CachedLessonType
import com.geridea.trentastico.utils.StringUtils
import java.util.*

class LessonType(
        val id: String,
        val name: String,
        val partitioningName: String,
        val color: Int,
        var isVisible: Boolean) {

    var partitioning: Partitioning = Partitioning.NONE

    constructor(cachedLessonType: CachedLessonType, isVisible: Boolean) : this(
            cachedLessonType.lesson_type_id,
            cachedLessonType.name,
            cachedLessonType.partitioningName,
            cachedLessonType.color,
            isVisible)

    fun findPartitioningsToHide(): ArrayList<PartitioningCase> =
            partitioning.cases.filterNotTo(ArrayList(4)) { it.isVisible }

    /**
     * Applies the current visibility to it's partitionings.
     * @return true if any partitioning has changed, false otherwise
     */
    fun applyVisibilityToPartitionings(): Boolean =
            partitioning.applyVisibilityToPartitionings(isVisible)

    fun hasAllPartitioningsInvisible(): Boolean = partitioning.hasAllPartitioningsInvisible()

    fun hasAtLeastOnePartitioningVisible(): Boolean =
            partitioning.hasAtLeastOnePartitioningVisible()

    fun mergePartitionings(partitioning: Partitioning) = partitioning.mergePartitionCases(partitioning)

    companion object {

        fun findPartitioningFromDescription(description: String): Partitioning {
            for (type in PartitioningType.values()) {
                val match = StringUtils.findMatchingStringIfAny(description, type.regex)

                if (match != null)
                    return Partitioning(type, match)
            }

            return Partitioning.NONE
        }

        fun getSortedLessonTypes(lessons: Collection<LessonType>): ArrayList<LessonType> {
            val lessonTypes = ArrayList(lessons)

            //Sort all the courses alphabetically
            Collections.sort(lessonTypes) { o1, o2 -> o1.name.compareTo(o2.name) }
            return lessonTypes
        }

        fun getColorFromCSSStyle(cssClassName: String): Int {
            when (cssClassName) {
                "colore1" -> return 0xFFFFEFAA.toInt()
                "colore2" -> return 0xFFFFF9AA.toInt()
                "colore3" -> return 0xFFFAFFAA.toInt()
                "colore4" -> return 0xFFF0FFAA.toInt()
                "colore5" -> return 0xFFE7FFAA.toInt()
                "colore6" -> return 0xFFDDFFAA.toInt()
                "colore7" -> return 0xFFD3FFAA.toInt()
                "colore8" -> return 0xFFC9FFAA.toInt()
                "colore9" -> return 0xFFBFFFAA.toInt()
                "colore10" -> return 0xFFB6FFAA.toInt()
                "colore11" -> return 0xFFACFFAA.toInt()
                "colore12" -> return 0xFFAAFFBD.toInt()
                "colore13" -> return 0xFFAAFFC6.toInt()
                "colore14" -> return 0xFFAAFFD0.toInt()
                "colore15" -> return 0xFFAAFFDA.toInt()
                "colore16" -> return 0xFFAAFFE4.toInt()
                "colore17" -> return 0xFFAAFFEE.toInt()
                "colore18" -> return 0xFFAAFFF7.toInt()
                "colore19" -> return 0xFFAAFCFF.toInt()
                "colore20" -> return 0xFFAAF2FF.toInt()
                "colore21" -> return 0xFFAAE8FF.toInt()
                "colore22" -> return 0xFFAADEFF.toInt()
                "colore23" -> return 0xFFAAD4FF.toInt()
                "colore24" -> return 0xFFAACBFF.toInt()
                "colore25" -> return 0xFFAAC1FF.toInt()
                "colore26" -> return 0xFFAAB7FF.toInt()
                "colore27" -> return 0xFFAAADFF.toInt()
                "colore28" -> return 0xFFB1AAFF.toInt()
                "colore29" -> return 0xFFBBAAFF.toInt()
                "colore30" -> return 0xFFC5AAFF.toInt()
                "colore31" -> return 0xFFCFAAFF.toInt()
                "colore32" -> return 0xFFD9AAFF.toInt()
                "colore33" -> return 0xFFE2AAFF.toInt()
                "colore34" -> return 0xFFECAAFF.toInt()
                "colore35" -> return 0xFFF6AAFF.toInt()
                "colore36" -> return 0xFFFFAAFD.toInt()
                "colore37" -> return 0xFFFFAAF3.toInt()
                "colore38" -> return 0xFFFFAAE9.toInt()
                "colore39" -> return 0xFFFFAAE0.toInt()
                "colore40" -> return 0xFFFFAAD6.toInt()
                "colore41" -> return 0xFFFFAACC.toInt()
                "colore42" -> return 0xFFFFAAC2.toInt()
                "colore43" -> return 0xFFFFAAB8.toInt()
                "colore44" -> return 0xFFFFAAAA.toInt()
                "colore45" -> return 0xFFFFB4AA.toInt()
                "colore46" -> return 0xFFFFBEAA.toInt()
                "colore47" -> return 0xFFFFC8AA.toInt()
                "colore48" -> return 0xFFFFD2AA.toInt()
                "colore49" -> return 0xFFFFDBAA.toInt()
                "colore50" -> return 0xFFFFE5AA.toInt()
            }

            return 0xFFFFFFFF.toInt()
        }
    }

}
