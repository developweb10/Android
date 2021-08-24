package com.app.tourguide.ui.avaliableplaces.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class DataItem(

        @field:SerializedName("Location")
        val location: Location? = null,

        @field:SerializedName("TourPackages")
        val tourPackages: List<TourPackagesItem?>? = null
) : Serializable