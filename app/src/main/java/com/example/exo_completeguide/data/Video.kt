package com.example.exo_completeguide.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val name: String,
    val uri: String,
    val adUri: String? = null,
    val adTagUri: String? = null,
    val subtitleConfiguration: String? = null
) : Parcelable

fun getTelewebionHls(): ArrayList<Video> {
    return arrayListOf(
        Video(
            "Telewebion HLS",
            "https://cdna.telewebion.com/varzesh/episode/0xd912a8d/playlist.m3u8"
        )
    )
}

fun getTelewebionPlayListHls(): ArrayList<Video> {
    return arrayListOf(
        Video(
            "Telewebion HLS1",
            "https://cdna.telewebion.com/varzesh/episode/0xd912a8d/playlist.m3u8"
        ),
        Video(
            "Telewebion HLS2",
            "https://cdna.telewebion.com/varzesh/episode/0xe7ec392/playlist.m3u8"
        ),
        Video(
            "Telewebion HLS3",
            "https://cdna.telewebion.com/varzesh/episode/0xe6e11f7/playlist.m3u8"
        ),
    )
}

fun getTelewebionLive(): ArrayList<Video> {
    return arrayListOf(
        Video(
            "Telewebion Live tv3",
            "https://ncdn.telewebion.com/tv3/live/playlist.m3u8"
        )
    )
}

fun getTelewebionLivePlayList(): ArrayList<Video> {
    return arrayListOf(
        Video(
            "Telewebion Live tv3",
            "https://ncdn.telewebion.com/tv3/live/playlist.m3u8"
        ),
        Video(
            "Telewebion Live tv4",
            "https://ncdn.telewebion.com/tv4/live/playlist.m3u8"
        )
    )
}
