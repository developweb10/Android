package com.app.tourguide.data.token

import com.google.gson.annotations.SerializedName

data class Data(

	@field:SerializedName("device_id")
	val deviceId: String? = null,

	@field:SerializedName("id")
	val id: String? = null,

	@field:SerializedName("package_id")
	val packageId: String? = null,

	@field:SerializedName("code_used")
	val codeUsed: String? = null
)