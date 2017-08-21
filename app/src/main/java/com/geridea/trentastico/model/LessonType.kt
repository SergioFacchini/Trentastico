package com.geridea.trentastico.model

import com.geridea.trentastico.model.cache.CachedLessonType

@Deprecated(message = "The lesson types shall be removed")
class LessonType(
        val id: String,
        val name: String,
        val partitioningName: String,
        val color: Int,
        var isVisible: Boolean) {

    constructor(cachedLessonType: CachedLessonType, isVisible: Boolean) : this(
            cachedLessonType.lesson_type_id,
            cachedLessonType.name,
            cachedLessonType.partitioningName,
            cachedLessonType.color,
            isVisible)

}
