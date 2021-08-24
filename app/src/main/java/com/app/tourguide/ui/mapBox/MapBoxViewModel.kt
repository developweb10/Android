package com.app.tourguide.ui.mapBox

import androidx.lifecycle.MutableLiveData
import com.app.tourguide.api.model.MyViewModel
import com.app.tourguide.data.token.TokenUpdateResponse
import com.app.tourguide.ui.mapBox.response.PackageSpotsResponse

class MapBoxViewModel : MyViewModel() {

    var response = MutableLiveData<PackageSpotsResponse>()
    var tokenResponse = MutableLiveData<TokenUpdateResponse>()

    fun getPackagesData(pckgId: String, deviceId: String) {
        isLoading.value = true
        MapBoxRepository.getPackageSpots({
            response.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, pckgId, deviceId)
    }


    fun updateTourTokenTime(data: HashMap<String, String>) {
        isLoading.value = true
        MapBoxRepository.updateTourTokenTime({
            tokenResponse.value = it
            isLoading.value = false
        }, {
            apiError.value = it
            isLoading.value = false
        }, {
            onFailure.value = it
            isLoading.value = false
        }, data)
    }

}