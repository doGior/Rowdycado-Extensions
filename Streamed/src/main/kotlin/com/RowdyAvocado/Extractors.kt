package com.RowdyAvocado

import android.util.Log
import com.lagradost.cloudstream3.APIHolder.capitalize
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class StreamedExtractor : ExtractorApi() {
    override val mainUrl = Streamed.mainUrl
    override val name = Streamed.name
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val TAG = "StreamedExtractor:getUrl"
        Log.d(TAG,"REFERER: $referer  URL: $url")

        if (url.isNotEmpty()) {
            try {
                val rawSource = app.get(url).body.string()
                Log.d(TAG, rawSource)
                val source = parseJson<List<Streamed.Source>>(rawSource)
                val serverDomain = "https://rr.vipstreams.in"
                if (source.isNotEmpty()) {
                    source.forEach{ s ->
                        val isHdString = if(s.isHD) "HD" else "SD"
                        val contentUrl = "$serverDomain/${s.source}/js/${s.id}/${s.streamNumber}/playlist.m3u8"
                        Log.d(TAG, "Content URL: $contentUrl")
                        callback.invoke(
                            ExtractorLink(
                                source = s.source!!,
                                name = "${s.source.capitalize()} $isHdString \t ${s.language}",
                                url = contentUrl,
                                referer = referer!!,
                                isM3u8 = true,
                                quality = Qualities.Unknown.value
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error: ${e.message}")
            }
        }

    }

}
