package com.example.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit.Builder
import java.util.concurrent.TimeUnit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkModule(private val context: Context) {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(MockApiInterceptor(context))
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    object RetrofitClient {
        private const val BASE_URL = "http://quiniela.jmacboy.com/"

        val api: ApiService by lazy {
            Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QuinielaApi::class.java)
        }
    }
