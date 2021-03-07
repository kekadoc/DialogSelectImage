package com.kekadoc.tools.android.dialog.example

import android.util.SparseArray
import com.kekadoc.tools.stick.sample.Sample

object AvatarsProvider {

    private val avatars = SparseArray<String>()
    private var customSize = 0

    const val EMPTY_INDEX = -1

    fun addCustomUrl(url: String) {
        avatars.put(customSize, url)
        customSize++
    }

    fun getImageUrl(index: Int): String {
        if (index < 0) return Sample.Image.Person.URL_UNKNOWN
        var imageUrl = avatars.get(index)
        if (imageUrl == null) {
            imageUrl = Sample.Image.Person.getRandomUrl()
            avatars.put(index, imageUrl)
        }
        return imageUrl
    }

}