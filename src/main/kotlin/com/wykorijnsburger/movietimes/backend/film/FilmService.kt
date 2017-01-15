package com.wykorijnsburger.movietimes.backend.film

import com.wykorijnsburger.movietimes.backend.client.cineville.CinevilleClient
import com.wykorijnsburger.movietimes.backend.client.cineville.emptyFilm
import com.wykorijnsburger.movietimes.backend.client.tmdb.TMDBClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import toFlux

@Service
class FilmService(private val cinevilleClient: CinevilleClient,
                  private val tmdbClient: TMDBClient) {

    fun getFilms(ids: List<String>): Flux<Film> {
        val cinevilleFilms = cinevilleClient.getFilms(ids.toSet()).cache()

//        val tmdbFilms: Flux<TMDBVideoResult> = cinevilleFilms.map { it.title }
//                .flatMap { tmdbClient.searchMovie(it) }
//                .flatMap {
//                    val filmId = it.results.firstOrNull()?.id
//                    if (filmId != null) {
//                        tmdbClient.getVideos(filmId)
//                    }
//
//                    TMDBVideoResult("test", "test", "test").toMono()
//                }

//        return Flux.zip(cinevilleFilms, tmdbFilms)
//                .map {
//                    Film(title = it.t1.title,
//                            language = it.t1.language,
//                            posterUrl = it.t1.poster,
//                            year = it.t1.year)
//                }

        val paddedFilms = cinevilleFilms.collectList().map {
            val filmMap = it.associateBy({ it.id }, { it })
            ids.map { id -> filmMap[id] ?: emptyFilm() }
        }.flatMap { it.toFlux() }

        return paddedFilms
                .map {
                    Film(title = it.title,
                            language = it.language,
                            posterUrl = it.poster,
                            year = it.year)
                }
    }
}