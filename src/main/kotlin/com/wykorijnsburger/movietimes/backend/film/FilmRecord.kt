package com.wykorijnsburger.movietimes.backend.film

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class FilmRecord(
        @Id
        val cinevilleId: Long,
        val title: String,
        val posterUrl: String? = null,
        val stillUrl: String? = null,
        val trailerUrl: String? = null,
        var directors: String,
        var cast: String,
        val language: String? = null,
        @Column(columnDefinition = "text")
        val oneLiner: String? = null,
        val year: String? = null,
        @Column(columnDefinition = "text")
        val teaser: String? = null,
        val runtime: Int? = null
)

fun FilmRecord.toDomain(): Film {
    return Film(
            cinevilleId = cinevilleId,
            title = title,
            posterUrl = posterUrl,
            stillUrl = stillUrl,
            trailerUrl = trailerUrl,
            directors = directors.split(","),
            cast = cast.split(","),
            language = language,
            oneLiner = oneLiner,
            year = year,
            teaser = teaser,
            runtime = runtime
    )
}

fun Film.toRecord(): FilmRecord {
    return FilmRecord(
            cinevilleId = cinevilleId,
            title = title,
            posterUrl = posterUrl,
            stillUrl = stillUrl,
            trailerUrl = trailerUrl,
            directors = directors.joinToString(","),
            cast = cast.joinToString(","),
            language = language,
            oneLiner = oneLiner,
            year = year,
            teaser = teaser,
            runtime = runtime
    )
}

