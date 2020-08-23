package com.example.sensorsstation.rest

import okhttp3.ResponseBody
import retrofit2.Callback

object SensorsDataCall {

    private val retrofitClient = RetrofitClientFactory().createRetrofit()

    private val sensorsDataAPI: SensorsDataAPI = retrofitClient.create(SensorsDataAPI::class.java)

    fun postSensorsData(sensorsData: SensorsData, callback: Callback<ResponseBody>) =
        sensorsDataAPI.postSensorsData(sensorsData).enqueue(callback)
}