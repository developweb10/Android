package com.app.tourguide.api.service

import com.app.tourguide.data.token.TokenResponse
import com.app.tourguide.data.token.TokenUpdateResponse
import com.app.tourguide.ui.avaliableplaces.model.ResponseAvailableTour
import com.app.tourguide.ui.avaliableplaces.pojomodel.ResponseAvaliablePlaces
import com.app.tourguide.ui.downloadPreview.pojomodel.ResponseAvailLang
import com.app.tourguide.ui.mapBox.response.PackageSpotsResponse
import com.app.tourguide.ui.placedetail.pojomodel.ResponsePlaceDetail
import com.app.tourguide.ui.tourLanguage.response.TourLangResponse
import com.app.tourguide.ui.tourLanguage.response.TourResponse
import com.app.tourguide.utils.Constants
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


/**
 *  All web services are declared here
 */
interface WebService {

    @GET("directions/json?")
    fun getPathDetails(@Query(value = "origin=", encoded = true)
                       source_location: String, @Query(value = "&destination=", encoded = true)
                       final_destination: String, @Query(value = "&sensor=true&key=", encoded = true) server_key: String): Call<ResponseBody>


    @FormUrlEncoded
    @POST(Constants.TOUR_PACKAGE_DETAILS)
    fun getPlaceDetail(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String): Call<ResponsePlaceDetail>

    @FormUrlEncoded
    @POST(Constants.URL_DOWNLOAD_PCKG)
    fun sendOtp(@Field("code") code: String, @Field("device_id") deviceId: String, @Field("package_id") packageId: String): Call<TokenResponse>

    @FormUrlEncoded
    @POST(Constants.AVAIL_LANGUAGE)
    fun getAvailLang(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String): Call<ResponseAvailLang>


    @FormUrlEncoded
    @POST(Constants.PREVIEW_LANGUAGE)
    fun getPreviewLang(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String): Call<ResponseAvailLang>


    @FormUrlEncoded
    @POST(Constants.URL_AVL_LOC)
    fun getAvaliablePlaces(@Field("device_id") deviceId: String): Call<ResponseAvaliablePlaces>

    @FormUrlEncoded
    @POST(Constants.URL_AVL_TOURS)
    fun getAvaliableTours(@Field("device_id") deviceId: String): Call<ResponseAvailableTour>

    @FormUrlEncoded
    @POST(Constants.URL_SAVE_DEVICE_ID)
    fun postDeviceId(@Field("device_id") deviceId: String, @Field("device_srno") macAddress: String): Call<ResponsePlaceDetail>

    @FormUrlEncoded
    @POST(Constants.URL_ACTIVE_LANG)
    fun postActiveLang(@Field("active_language") actLang: String, @Field("device_id") deviceId: String): Call<ResponseAvailLang>


    @FormUrlEncoded
    @POST(Constants.AVAIL_LANGUAGE)
    fun getTourLanguages(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String): Call<TourResponse>


    @FormUrlEncoded
    @POST(Constants.URL_PCKG_SPOTS)
    fun getPackageSpots(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String): Call<PackageSpotsResponse>


    @FormUrlEncoded
    @POST(Constants.TOUR_LANGUAGE)
    fun updateTourLanguages(@Field("package_id") pckgId: String, @Field("device_id") deviceId: String, @Field("active_package_language") packageId: String): Call<TourLangResponse>

    @FormUrlEncoded
    @POST(Constants.URL_UPDATE_TOKEN_TIME)
    fun updateTourTokenTime(@FieldMap params: Map<String, String>): Call<TokenUpdateResponse>


}