package com.wykorijnsburger.movietimes.backend.film

import com.wykorijnsburger.movietimes.backend.client.cineville.CinevilleClient
import com.wykorijnsburger.movietimes.backend.client.cineville.CinevilleFilm
import com.wykorijnsburger.movietimes.backend.client.tmdb.TMDBClient
import com.wykorijnsburger.movietimes.backend.client.tmdb.TMDBDetailsResult
import com.wykorijnsburger.movietimes.backend.client.tmdb.TMDBSearchResult
import com.wykorijnsburger.movietimes.backend.showtime.ShowtimeRecord
import com.wykorijnsburger.movietimes.backend.utils.orNull
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.util.function.component1
import reactor.util.function.component2
import java.time.Duration
import java.util.*

@Service
class FilmService(private val cinevilleClient: CinevilleClient,
                  private val tmdbClient: TMDBClient,
                  private val filmRepository: FilmRepository) {

    private val logger = KotlinLogging.logger {}

    fun updateFilms(showtimes: List<ShowtimeRecord>) {
        val filmIds = showtimes
                .map { it.filmId }
                .toSet()

        getFilms(filmIds)
                .doOnError { logger.error(it) { "Error retrieving films: $it" } }
                .map { it.toRecord() }
                .subscribe(
                        {
                            val filmExistsInDB = filmRepository.existsById(it.cinevilleId)
                            if (filmExistsInDB) {
                                filmRepository.deleteById(it.cinevilleId)
                            }
                            filmRepository.save(it)
                        }, {
                            logger.error { "Error saving film to DB: $it" }
                        }
                )
    }

    fun getFilmsFromDb(): Flux<Film> {
        return filmRepository.findAll()
                .toFlux()
                .map { it.toDomain() }
    }

    fun getCinevilleFilms(ids: List<String>): Flux<Optional<CinevilleFilm>> {
        return cinevilleClient.getFilms(ids.toSet())
                .collectList()
                .map {
                    val filmMap = it.associateBy({ it.id }, { it })

                    ids.map { id -> Optional.ofNullable(filmMap[id]) }
                }
                .flatMapIterable { it }
    }

    private fun getFilms(ids: Set<String>): Flux<Film> {
        val cinevilleFilms = cinevilleClient.getFilms(ids = ids)
                .cache()

        val tmdbFilms: Flux<Optional<TMDBDetailsResult>> = cinevilleFilms.map { it.title }
                .delayElements(Duration.ofMillis(1000))
                .flatMap<TMDBSearchResult> { tmdbClient.searchMovie(it) }
                .flatMap {
                    val id = it.results.firstOrNull()?.id
                    if (id != null) {
                        tmdbClient.getMovieDetailsWithVideos(id)
                                .map { Optional.ofNullable(it) }
                    } else {
                        Mono.just(Optional.empty<TMDBDetailsResult>())
                    }
                }

        return Flux.zip(cinevilleFilms, tmdbFilms)
                .map { (cinevilleFilm, tmdbFilm) -> compose(cinevilleFilm, tmdbFilm.orNull()) }
    }
}