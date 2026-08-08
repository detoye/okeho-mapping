package com.okeho.mapping.domain.model

enum class FeatureType(val displayName: String, val icon: String) {
    BUSINESS("Business", "store"),
    LANDMARK("Landmark", "location_city"),
    SCHOOL("School", "school"),
    HEALTH_FACILITY("Health facility", "local_hospital"),
    BUILDING("Building", "apartment"),
    TRANSPORT("Transport", "directions_bus"),
    ROAD_FEATURE("Road feature", "road"),
    OTHER("Other", "more_horiz");

    companion object {
        fun fromString(value: String): FeatureType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
