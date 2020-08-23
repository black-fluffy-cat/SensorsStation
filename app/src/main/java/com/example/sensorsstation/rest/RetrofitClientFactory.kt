package com.example.sensorsstation.rest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

private const val serverUrl = "http://192.168.0.108:8080/"

class RetrofitClientFactory {

    fun createRetrofit(): Retrofit = Retrofit.Builder().baseUrl(serverUrl).client(createHttpClient())
        .addConverterFactory(JacksonConverterFactory.create(createMapper())).build()

    private fun createHttpClient() = OkHttpClient.Builder().apply {
        retryOnConnectionFailure(true)
        readTimeout(1, TimeUnit.MINUTES)
        connectTimeout(1, TimeUnit.MINUTES)
    }.build()

    private fun createMapper() = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}