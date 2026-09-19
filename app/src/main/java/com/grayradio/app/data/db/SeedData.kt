package com.grayradio.app.data.db

import com.grayradio.app.data.entity.Station

object SeedData {
    val stations: List<Station> = listOf(
        Station(
            name = "Retro Rádió",
            streamUrl = "https://icast.connectmedia.hu/5001/live.mp3",
            sortOrder = 0,
            countryCode = "HU",
        ),
        Station(
            name = "ROCK FM",
            streamUrl = "https://icast.connectmedia.hu/5301/live.mp3/",
            sortOrder = 1,
            countryCode = "HU",
            tags = "rock",
        ),
        Station(
            name = "BDPST ROCK (320)",
            streamUrl = "http://s2.audiostream.hu/bdpstrock_320k",
            sortOrder = 2,
            countryCode = "HU",
            tags = "rock",
        ),
        Station(
            name = "Petőfi Rádió",
            streamUrl = "https://icast.connectmedia.hu/4738/mr2.mp3",
            sortOrder = 3,
            countryCode = "HU",
        ),
        Station(
            name = "Klubrádió",
            streamUrl = "https://a7.asurahosting.com:8160/radio.mp3",
            sortOrder = 4,
            countryCode = "HU",
            tags = "news,talk",
        ),
    )
}
