package com.app.tourguide.callBack

import android.net.Uri
import org.jetbrains.annotations.NotNull


interface ItemSelectedListener {

    fun selectedItem(pos: Int, type: @NotNull String, url: Uri)


}
