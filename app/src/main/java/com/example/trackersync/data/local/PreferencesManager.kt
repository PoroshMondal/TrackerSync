package com.example.trackersync.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.trackersync.domain.model.AttendanceRecord
import com.example.trackersync.domain.model.OfficeLocation
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "tracker_sync_prefs"
        private const val KEY_OFFICE_LAT = "office_lat"
        private const val KEY_OFFICE_LNG = "office_lng"
        private const val KEY_OFFICE_ADDRESS = "office_address"
        private const val KEY_OFFICE_UPDATED_AT = "office_updated_at"
        private const val KEY_ATTENDANCE_HISTORY_JSON = "attendance_history_json"
    }

    fun saveOfficeLocation(location: OfficeLocation) {
        prefs.edit()
            .putFloat(KEY_OFFICE_LAT, location.latitude.toFloat())
            .putFloat(KEY_OFFICE_LNG, location.longitude.toFloat())
            .putString(KEY_OFFICE_ADDRESS, location.address)
            .putLong(KEY_OFFICE_UPDATED_AT, location.timestamp)
            .apply()
    }

    fun getOfficeLocation(): OfficeLocation? {
        if (!prefs.contains(KEY_OFFICE_LAT) || !prefs.contains(KEY_OFFICE_LNG)) {
            return null
        }
        val lat = prefs.getFloat(KEY_OFFICE_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_OFFICE_LNG, 0f).toDouble()
        val address = prefs.getString(KEY_OFFICE_ADDRESS, "Saved Office Location") ?: "Saved Office Location"
        val timestamp = prefs.getLong(KEY_OFFICE_UPDATED_AT, System.currentTimeMillis())
        return OfficeLocation(latitude = lat, longitude = lng, address = address, timestamp = timestamp)
    }

    fun clearOfficeLocation() {
        prefs.edit()
            .remove(KEY_OFFICE_LAT)
            .remove(KEY_OFFICE_LNG)
            .remove(KEY_OFFICE_ADDRESS)
            .remove(KEY_OFFICE_UPDATED_AT)
            .apply()
    }

    fun saveAttendanceRecord(record: AttendanceRecord) {
        val currentHistory = getAttendanceHistory().toMutableList()
        currentHistory.add(0, record) // Newest first

        val jsonArray = JSONArray()
        for (item in currentHistory) {
            val jsonObject = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("userLatitude", item.userLatitude)
                put("userLongitude", item.userLongitude)
                put("officeLatitude", item.officeLatitude)
                put("officeLongitude", item.officeLongitude)
                put("distanceMeters", item.distanceMeters)
                put("isWithinGeofence", item.isWithinGeofence)
                put("note", item.note)
            }
            jsonArray.put(jsonObject)
        }

        prefs.edit().putString(KEY_ATTENDANCE_HISTORY_JSON, jsonArray.toString()).apply()
    }

    fun getAttendanceHistory(): List<AttendanceRecord> {
        val jsonString = prefs.getString(KEY_ATTENDANCE_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<AttendanceRecord>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AttendanceRecord(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        userLatitude = obj.getDouble("userLatitude"),
                        userLongitude = obj.getDouble("userLongitude"),
                        officeLatitude = obj.getDouble("officeLatitude"),
                        officeLongitude = obj.getDouble("officeLongitude"),
                        distanceMeters = obj.getDouble("distanceMeters"),
                        isWithinGeofence = obj.getBoolean("isWithinGeofence"),
                        note = obj.optString("note", "Geo-fenced check-in")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
