package de.shinz.rickandmortyshowcase

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform