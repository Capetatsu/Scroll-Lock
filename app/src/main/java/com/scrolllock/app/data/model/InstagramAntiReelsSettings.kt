package com.scrolllock.app.data.model

data class InstagramAntiReelsSettings(
    val hideReelsOnHome: Boolean = true,
    val blockExplore: Boolean = true,
    val blockMainFeed: Boolean = false,
    val blockStories: Boolean = false,
    val blockComments: Boolean = false,
    val allowReelsInDMs: Boolean = true,
    val redirectOnBlock: Boolean = false
)
