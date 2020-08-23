package com.example.sensorsstation.rest

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SensorsDataAPI {

    @POST("sensors/data")
    fun postSensorsData(@Body sensorsData: SensorsData): Call<ResponseBody>
}