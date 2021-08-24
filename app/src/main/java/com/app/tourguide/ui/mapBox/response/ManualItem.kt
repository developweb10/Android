package com.app.tourguide.ui.mapBox.response

import com.google.gson.annotations.SerializedName

data class ManualItem(

	@field:SerializedName("code")
	val code: String? = null,

	@field:SerializedName("trips")
	val trips: List<List<String?>?>? = null
)