package com.example.medtest1.doctor

import com.example.medtest1.data.TreatmentPlan
import com.example.medtest1.data.UserProfile
import com.example.medtest1.data.WellbeingEntry
import org.json.JSONArray
import org.json.JSONObject

fun userProfileToJson(profile: UserProfile?): String {
    if (profile == null) return "{}"
    return JSONObject()
        .put("username", profile.username)
        .put("fullName", profile.fullName)
        .put("birthDate", profile.birthDate)
        .put("gender", profile.gender)
        .put("bloodType", profile.bloodType)
        .put("allergies", profile.allergies)
        .put("chronicDiseases", profile.chronicDiseases)
        .put("regularMedications", profile.regularMedications)
        .put("relativeContact", profile.relativeContact)
        .put("weight", profile.weight)
        .put("height", profile.height)
        .toString()
}

fun treatmentDataToJson(
    plans: List<TreatmentPlan>,
    wellbeing: Map<String, WellbeingEntry>
): String {
    val plansArr = JSONArray()
    plans.forEach { p ->
        plansArr.put(
            JSONObject()
                .put("medicineName", p.medicineName)
                .put("dosage", p.dosage)
                .put("reminderTime", p.reminderTime)
                .put("startDate", p.startDate)
                .put("endDate", p.endDate)
                .put("notes", p.notes)
        )
    }
    val wellbeingObj = JSONObject()
    wellbeing.forEach { (date, entry) ->
        wellbeingObj.put(
            date,
            JSONObject()
                .put("mood", entry.status)
                .put("comment", entry.comment)
        )
    }
    return JSONObject()
        .put("plans", plansArr)
        .put("wellbeing", wellbeingObj)
        .toString()
}

fun reportDataToJson(
    username: String,
    displayName: String?,
    birthDate: String?,
    plans: List<TreatmentPlan>,
    wellbeing: Map<String, WellbeingEntry>
): String = JSONObject()
    .put("username", username)
    .put("displayName", displayName.orEmpty())
    .put("birthDate", birthDate.orEmpty())
    .put("treatment", JSONObject(treatmentDataToJson(plans, wellbeing)))
    .toString()
