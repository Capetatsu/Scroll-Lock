package com.scrolllock.app.data.room

import androidx.room.TypeConverter
import com.scrolllock.app.data.model.InstagramAntiReelsSettings
import com.scrolllock.app.data.model.TimeSlot
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromInstagramSettings(settings: InstagramAntiReelsSettings?): String? {
        return settings?.let {
            JSONObject().apply {
                put("hideReelsOnHome", it.hideReelsOnHome)
                put("blockExplore", it.blockExplore)
                put("blockMainFeed", it.blockMainFeed)
                put("blockStories", it.blockStories)
                put("blockComments", it.blockComments)
                put("allowReelsInDMs", it.allowReelsInDMs)
                put("redirectOnBlock", it.redirectOnBlock)
            }.toString()
        }
    }

    @TypeConverter
    fun toInstagramSettings(json: String?): InstagramAntiReelsSettings? {
        return json?.let {
            val obj = JSONObject(it)
            InstagramAntiReelsSettings(
                hideReelsOnHome = obj.optBoolean("hideReelsOnHome", true),
                blockExplore = obj.optBoolean("blockExplore", true),
                blockMainFeed = obj.optBoolean("blockMainFeed", false),
                blockStories = obj.optBoolean("blockStories", false),
                blockComments = obj.optBoolean("blockComments", false),
                allowReelsInDMs = obj.optBoolean("allowReelsInDMs", true),
                redirectOnBlock = obj.optBoolean("redirectOnBlock", false)
            )
        }
    }

    @TypeConverter
    fun fromTimeSlots(slots: List<TimeSlot>): String {
        val arr = JSONArray()
        for (slot in slots) {
            arr.put(JSONObject().apply {
                put("startHour", slot.startHour)
                put("startMinute", slot.startMinute)
                put("endHour", slot.endHour)
                put("endMinute", slot.endMinute)
            })
        }
        return arr.toString()
    }

    @TypeConverter
    fun toTimeSlots(json: String): List<TimeSlot> {
        val arr = JSONArray(json)
        val slots = mutableListOf<TimeSlot>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            slots.add(TimeSlot(
                startHour = obj.getInt("startHour"),
                startMinute = obj.getInt("startMinute"),
                endHour = obj.getInt("endHour"),
                endMinute = obj.getInt("endMinute")
            ))
        }
        return slots
    }
}
