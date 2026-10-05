package de.shinz.rickandmortyshowcase.core.data.favorite

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterDao
import de.shinz.rickandmortyshowcase.core.database.FavoriteCharacterEntity
import de.shinz.rickandmortyshowcase.core.domain.DataError
import de.shinz.rickandmortyshowcase.core.domain.Result
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The data source over a fake DAO.
 *
 * This covers what the data source itself adds on top of the DAO — the operator
 * chain whose obligations fail silently: a missing `catch` crashes the Favorites
 * screen, a missing `distinctUntilChanged` recomposes the whole list on every
 * unrelated write, and a missing `retry` makes one transient failure permanent.
 *
 * Anything that is really SQLite's behaviour — the `ORDER BY`, a missing row
 * being `null`, deleting an absent row — belongs in `FavoriteCharacterDaoTest`
 * against a real database, because asserting it against a fake only restates how
 * the fake was written.
 */
class RoomFavoriteCharacterLocalDataSourceTest {

    @Test
    fun observeFavouritesMapsRowsToDomainModels() = runTest {
        val dao = FakeFavoriteCharacterDao()
        dao.rows.value = listOf(MORTY.toFavoriteEntity(ADDED_AT))

        dataSource(dao).observeFavorites().test {
            assertThat(awaitItem()).isEqualTo(listOf(MORTY))
        }
    }

    @Test
    fun aFailingFavouritesQueryDegradesToEmptyInsteadOfCrashing() = runTest {
        // No error channel on this Flow by contract, so the alternative to this
        // is a raw exception reaching the collector.
        val dao = FakeFavoriteCharacterDao(
            allOverride = flow { throw IllegalStateException("database is closed") },
        )

        dataSource(dao).observeFavorites().test {
            assertThat(awaitItem()).isEqualTo(emptyList())
            // Degraded, *not* finished: `catch` used to complete the flow here,
            // which under `stateIn(Eagerly)` meant the screen never recovered.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aMalformedEpisodeColumnDegradesToEmptyToo() = runTest {
        // The JSON column is the one row value that can be corrupt on read.
        val broken = MORTY.toFavoriteEntity(ADDED_AT).copy(episodeUrlsJson = "not json")
        val dao = FakeFavoriteCharacterDao()
        dao.rows.value = listOf(broken)

        dataSource(dao).observeFavorites().test {
            assertThat(awaitItem()).isEqualTo(emptyList())
            // Degraded, *not* finished: `catch` used to complete the flow here,
            // which under `stateIn(Eagerly)` meant the screen never recovered.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFavouriteIdsDropsRepeatedEmissions() = runTest {
        // Room re-runs this query on any write to the table, so the same id set
        // arrives again and again. Without distinctUntilChanged every one of
        // those would recompose the character list.
        val ids = MutableSharedFlow<List<Int>>(replay = 1)
        val dao = FakeFavoriteCharacterDao(idsOverride = ids)

        dataSource(dao).observeFavoriteIds().test {
            ids.emit(listOf(1, 2))
            assertThat(awaitItem()).isEqualTo(setOf(1, 2))

            ids.emit(listOf(2, 1)) // same set, different order
            ids.emit(listOf(1, 2)) // literally the same
            expectNoEvents()

            ids.emit(listOf(1, 2, 3))
            assertThat(awaitItem()).isEqualTo(setOf(1, 2, 3))
        }
    }

    @Test
    fun aFailingIdsQueryDegradesToEmptyToo() = runTest {
        // The contract puts catch on BOTH observe functions; only the favourites
        // one was covered before.
        val dao = FakeFavoriteCharacterDao(
            idsOverride = flow { throw IllegalStateException("database is closed") },
        )

        dataSource(dao).observeFavoriteIds().test {
            assertThat(awaitItem()).isEqualTo(emptySet())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addFavouriteStampsTheRowWithTheInjectedClock() = runTest {
        val dao = FakeFavoriteCharacterDao()

        dataSource(dao).addFavorite(MORTY)

        assertThat(dao.rows.value.single().addedAtEpochMillis)
            .isEqualTo(ADDED_AT.toEpochMilliseconds())
    }

    @Test
    fun aFullDiskIsReportedAsSuch() = runTest {
        // SQLite's own wording for SQLITE_FULL. The message is the only signal
        // androidx.sqlite exposes, so this is what the classifier matches.
        val dao = FakeFavoriteCharacterDao(
            // The exact text androidx.sqlite's throwSQLiteException builds:
            // "Error code: $errorCode, message: $errorMsg".
            upsertError = IllegalStateException("Error code: 13, message: database or disk is full"),
        )

        val result = dataSource(dao).addFavorite(MORTY)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Local.DISK_FULL)
    }

    @Test
    fun anyOtherStorageFailureIsUnknown() = runTest {
        val dao = FakeFavoriteCharacterDao(
            upsertError = IllegalStateException("constraint violation"),
        )

        val result = dataSource(dao).addFavorite(MORTY)

        assertThat((result as Result.Error).error).isEqualTo(DataError.Local.UNKNOWN)
    }

    private fun dataSource(dao: FavoriteCharacterDao) =
        RoomFavoriteCharacterLocalDataSource(dao, FixedClock)

    private object FixedClock : Clock {
        override fun now(): Instant = ADDED_AT
    }

    private class FakeFavoriteCharacterDao(
        private val allOverride: Flow<List<FavoriteCharacterEntity>>? = null,
        private val idsOverride: Flow<List<Int>>? = null,
        private val upsertError: Throwable? = null,
    ) : FavoriteCharacterDao {

        val rows = MutableStateFlow<List<FavoriteCharacterEntity>>(emptyList())

        override fun observeAll(): Flow<List<FavoriteCharacterEntity>> = allOverride ?: rows

        override fun observeIds(): Flow<List<Int>> =
            idsOverride ?: rows.map { entities -> entities.map { it.id } }

        override suspend fun getById(id: Int): FavoriteCharacterEntity? =
            rows.value.firstOrNull { it.id == id }

        override suspend fun upsert(entity: FavoriteCharacterEntity) {
            upsertError?.let { throw it }
            rows.value = rows.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun deleteById(id: Int) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    private companion object {
        val ADDED_AT = Instant.fromEpochMilliseconds(1_700_000_000_000L)

        val MORTY = Character(
            id = 2,
            name = "Morty Smith",
            status = CharacterStatus.ALIVE,
            species = "Human",
            type = "",
            gender = "Male",
            originName = "unknown",
            originUrl = "",
            locationName = "Citadel of Ricks",
            locationUrl = "loc/3",
            imageUrl = "avatar/2.jpeg",
            episodeUrls = listOf("ep/1", "ep/2"),
            url = "character/2",
            created = Instant.parse("2017-11-04T18:48:46.250Z"),
        )
    }
}
