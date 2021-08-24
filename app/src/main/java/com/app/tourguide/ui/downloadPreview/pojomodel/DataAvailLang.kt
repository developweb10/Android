package com.app.tourguide.ui.downloadPreview.pojomodel

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DataAvailLang(

        @field:SerializedName("language")
        val language: String? = null,

        @field:SerializedName("language_id")
        val languageId: String? = null,

        @field:SerializedName("icon")
        val icon: String? = null,

        @field:SerializedName("file_type")
        var fileType: String? = null,

        @field:SerializedName("internet_link")
        val internetLink: String? = null,

        @field:SerializedName("video_url")
        val videoUrl: String? = null,

        @field:SerializedName("progress")
        var progress: Int? = 0,

        @field:SerializedName("status")
        var status: String? = "Download"

) : Serializable