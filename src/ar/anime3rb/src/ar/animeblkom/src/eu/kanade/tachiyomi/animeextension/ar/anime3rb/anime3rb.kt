package eu.kanade.tachiyomi.animeextension.ar.anime3rb

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class Anime3rb : ParsedAnimeSource( ) {

    override val name = "Anime3rb"

    override val baseUrl = "https://anime3rb.com"

    override val lang = "ar"

    override val supportsLatest = true

    // ============================== Popular ===============================
    override fun popularAnimeRequest(page: Int ): Request {
        return GET("$baseUrl/titles/list?page=$page", headers)
    }

    override fun popularAnimeSelector(): String = "div.grid-cols-2 > div.title-card"

    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        anime.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        anime.thumbnail_url = element.selectFirst("img")!!.attr("src")
        anime.title = element.selectFirst("h3")!!.text()
        return anime
    }

    override fun popularAnimeNextPageSelector(): String = "a[rel=next]"

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/titles/list?page=$page", headers)
    }

    override fun latestUpdatesSelector(): String = popularAnimeSelector()

    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // =============================== Search ===============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = "$baseUrl/titles/list".toHttpUrl().newBuilder()
            .addQueryParameter("query", query)
            .addQueryParameter("page", page.toString())
            .build()
        return GET(url.toString(), headers)
    }

    override fun searchAnimeSelector(): String = popularAnimeSelector()

    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // =========================== Anime Details ============================ 
    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.selectFirst("h1.text-2xl")!!.text()
        anime.thumbnail_url = document.selectFirst("img.w-full")!!.attr("src")
        anime.description = document.selectFirst("div.prose > p")!!.text()
        anime.genre = document.select("div.flex.flex-wrap.gap-2 > a").joinToString { it.text() }
        return anime
    }

    // ============================== Episodes ============================== 
    override fun episodeListSelector(): String = "div.episode-card"

    override fun episodeListFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        episode.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        episode.name = element.selectFirst("h3")!!.text()
        return episode
    }

    // ============================ Video Links ============================= 
    override fun videoListSelector(): String = "div.player-button > a"

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videoList = mutableListOf<Video>()

        document.select(videoListSelector()).forEach { element ->
            val videoUrl = element.attr("href")
            val videoName = element.text()
            // Assuming the video URL is directly playable or can be extracted from the href
            // For more complex sites, you might need to make another request to the videoUrl
            videoList.add(Video(videoUrl, videoName, videoUrl))
        }
        return videoList
    }

    override fun videoUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    override fun videoFromElement(element: Element): Video = throw UnsupportedOperationException("Not used")

    // ============================= Utilities ============================== 
    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
        .add("Referer", baseUrl)
}
