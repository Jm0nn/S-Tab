package com.ssafy.stab.data.note.request

import com.google.gson.annotations.SerializedName

data class PageId(
    @SerializedName("beforePageId")
    val beforePageId: String
)