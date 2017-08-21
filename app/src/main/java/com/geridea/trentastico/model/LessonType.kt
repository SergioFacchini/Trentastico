package com.geridea.trentastico.model

import com.geridea.trentastico.model.cache.CachedLessonType
import com.geridea.trentastico.utils.StringUtils
import java.util.*

@Deprecated(message = "The lesson types shall be removed")
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
    }

}
