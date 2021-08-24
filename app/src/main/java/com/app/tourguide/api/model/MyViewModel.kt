package com.app.tourguide.api.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Created by android on 1/3/18.
 */
open class MyViewModel : ViewModel() {

    var apiError = MutableLiveData<String>()
    var onFailure = MutableLiveData<Throwable>()
    var badRequest = MutableLiveData<String>()
    var isLoading = MutableLiveData<Boolean>()
    var isPullToRefreshLoading = MutableLiveData<Boolean>()

}