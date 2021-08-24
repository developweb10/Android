package com.app.tourguide.data.token

import com.google.gson.annotations.SerializedName

data class TokenUpdateResponse(

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("type")
	val type: String? = null,

	@field:SerializedName("status")
	val status: Int? = null
)