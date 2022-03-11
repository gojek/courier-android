package com.gojek.courier.authhttp.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Url

internal interface ApiService {
    @GET
    fun authenticate(@Url url: String, @HeaderMap headerMap: Map<String, String>): Call<ResponseBody>
}