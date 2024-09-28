@file:Suppress("PackageName")

package com.RowdyAvocado

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.APIHolder.capitalize
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LiveSearchResponse
import com.lagradost.cloudstream3.LiveStreamLoadResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app

import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink


class Streamed(val plugin: StreamedPlugin) : MainAPI() {
    override var mainUrl = Streamed.mainUrl
    override var name = Streamed.name
    override var supportedTypes = setOf(TvType.Live)
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = false

    companion object {
        val mainUrl = "https://streamed.su"
        var name = "Streamed"
        val refHeader = mapOf("Referer" to mainUrl)
    }

    private val sectionNamesList = mainPageOf(
        "$mainUrl/api/matches/live/popular" to "Popular",
        "$mainUrl/api/matches/football" to "Football",
        "$mainUrl/api/matches/baseball" to "Baseball",
        "$mainUrl/api/matches/american-football" to "American Football",
        "$mainUrl/api/matches/hockey" to "Hockey",
        "$mainUrl/api/matches/basketball" to "Basketball",
        "$mainUrl/api/matches/motor-sports" to "Motor Sports",
        "$mainUrl/api/matches/fight" to "Fight",
        "$mainUrl/api/matches/tennis" to "Tennis",
        "$mainUrl/api/matches/rugby" to "Rugby",
        "$mainUrl/api/matches/golf" to "Golf",
        "$mainUrl/api/matches/billiards" to "Billiards",
        "$mainUrl/api/matches/afl" to "AFL",
        "$mainUrl/api/matches/darts" to "Darts",
        "$mainUrl/api/matches/cricket" to "Cricket",
        "$mainUrl/api/matches/other" to "Other",
    )

    override val mainPage = sectionNamesList

    private fun searchResponseBuilder(
        listJson: List<Match>,
        filter: (Match) -> Boolean
    ): List<LiveSearchResponse> {
        return listJson.filter(filter).map { match ->
            var url = "null"
            if (match.matchSources.isNotEmpty()) {
                val sourceName = match.matchSources[0].sourceName
                val id = match.matchSources[0].id
                url = "$mainUrl/api/stream/$sourceName/$id"
            }

            LiveSearchResponse(
                name = match.title,
                url = url,
                apiName = this@Streamed.name,
                posterUrl = "$mainUrl${match.posterPath ?: "/api/images/poster/fallback.png"}"
            )
        }
    }

    /**
     * Gets the sources from the API
     */
    private suspend fun Match.getSources(): Map<String, List<Source>> {

        var sourceObjectList: LinkedHashMap<String, List<Source>> = linkedMapOf()
        this.matchSources.forEach { (source, id) ->
            sourceObjectList[source] =
                parseJson<List<Source>>(app.get("$mainUrl/api/stream/${source}/$id").body.string())
        }

        return sourceObjectList
    }


    //Get the Homepage
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val TAG = "STREAMED:MainPage"
        Log.d(TAG, "Url:${request.data}")
        val rawList = app.get(request.data).body.string()
        val listJson = parseJson<List<Match>>(rawList)

        Log.d(TAG, "Element: ${listJson[0]}")
        Log.d(TAG, "Teams: ${listJson[0].teams}")
        Log.d(TAG, "Sources: ${listJson[0].getSources().values}")


        val list = searchResponseBuilder(listJson) { match ->
            match.matchSources.isNotEmpty() && match.popular
        }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = list,
                isHorizontalImages = false
            ), false
        )
    }


    // This function gets called when you search for something also
    //This is to get Title,Href,Posters for Homepage
    override suspend fun search(query: String): List<SearchResponse> {
        val allMatches = app.get("$mainUrl/api/matches/all").body.string()
        val allMatchesJson = parseJson<List<Match>>(allMatches)
        val searchResults = searchResponseBuilder(allMatchesJson) { match ->
            match.matchSources.isNotEmpty() && match.title.contains(query, true)
        }
        return searchResults
    }

    private suspend fun Source.getMatch(id: String): Match? {
        val allMatches = app.get("$mainUrl/api/matches/all").body.string()
        val allMatchesJson = parseJson<List<Match>>(allMatches)
        val matchesList = allMatchesJson.filter { match ->
            match.matchSources.isNotEmpty() &&
                    match.matchSources.any { it.id == id && it.sourceName == this.source }
        }
        if (matchesList.isEmpty()) {
            return null
        }
        return matchesList[0]
    }

    // This function gets called when you enter the page/show
    override suspend fun load(url: String): LoadResponse {
        val TAG = "STREAMED:Item"

        Log.d(TAG, "URL: $url")
        if (url == "null") {
            throw ErrorLoadingException("The stream is not available")
        }
        val request = app.get(url)
        if (!request.isSuccessful) {
            Log.d(TAG, "CODE: ${request.code}")
            throw ErrorLoadingException("Cannot get info from the API")
        }

        val rawJson = request.body.string()
        Log.d(TAG, "Response: ${app.get(url)}")
        val data = parseJson<List<Source>>(rawJson)

        var elementName = "No Info Yet"
        var elementPlot: String? = null
        var elementPoster: String? = null
        var elementTags: ArrayList<String> = arrayListOf()
        if (data.isNotEmpty()) {
            val sourceID = url.substringAfterLast('/')
            Log.d(TAG, "Source ID: $sourceID")
            val relatedMatch = data[0].getMatch(sourceID)
            if (relatedMatch != null) {
                val isHD = if (data[0].isHD) "HD" else ""
                elementName = relatedMatch.title
                elementPlot = "${relatedMatch.title}      $isHD"
                elementPoster =
                    "$mainUrl${relatedMatch.posterPath ?: "/api/images/poster/fallback.png"}"
                elementTags = arrayListOf(relatedMatch.category.capitalize())
                val teams = relatedMatch.teams?.values?.mapNotNull { it!!.name!! }
                if (teams != null) {
                    elementTags.addAll(teams)
                }
            } else {
                elementName = data[0].id.toString()
            }
        }


        val liveStream = LiveStreamLoadResponse(
            name = elementName,
            url = url,
            apiName = this.name,
            dataUrl = url,
            plot = elementPlot,
            posterUrl = elementPoster,
            tags = elementTags
        )
        Log.d(TAG, "LiveStream: $liveStream")
        return liveStream
    }

    // This function is how you load the links
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val TAG = "STREAMED:Links"

//        Log.d(TAG, "Url : $data")
        val rawJson = app.get(data).body.string()
        val source = parseJson<List<Source>>(rawJson)[0]

        val sourceUrlID = data.substringAfterLast("/")
//        Log.d(TAG, "Source ID: $sourceUrlID")

        val match = source.getMatch(sourceUrlID)
//        Log.d(TAG, "Match: $match")

        match?.matchSources?.forEach { matchSource ->
            Log.d(TAG, "URLS: $mainUrl/api/stream/${matchSource.sourceName}/${matchSource.id}")
            StreamedExtractor().getUrl(
                url = "$mainUrl/api/stream/${matchSource.sourceName}/${matchSource.id}",
                referer = "https://embedme.top/",
                subtitleCallback = subtitleCallback,
                callback = callback
            )
        }
        return true
    }

    data class Match(
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("title") val title: String,
        @JsonProperty("category") val category: String,
        @JsonProperty("date") val isoDateTime: Long? = null,
        @JsonProperty("poster") val posterPath: String? = null,
        @JsonProperty("popular") val popular: Boolean = false,
        @JsonProperty("teams") val teams: LinkedHashMap<String, Team?>? = null,
        @JsonProperty("sources") val matchSources: ArrayList<MatchSource> = arrayListOf(),
    )

    data class Team(
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("badge") val badge: String? = null,
    )

    data class MatchSource(
        @JsonProperty("source") val sourceName: String,
        @JsonProperty("id") val id: String
    )

    data class Source(
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("streamNo") val streamNumber: Int? = null,
        @JsonProperty("language") val language: String? = null,
        @JsonProperty("hd") val isHD: Boolean = false,
        @JsonProperty("embedUrl") val embedUrl: String? = null,
        @JsonProperty("source") val source: String? = null,
    )
}
