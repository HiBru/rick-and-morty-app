package de.shinz.rickandmortyshowcase.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import de.shinz.rickandmortyshowcase.core.data.favorite.toCharacter
import de.shinz.rickandmortyshowcase.core.data.favorite.toFavoriteEntity
import de.shinz.rickandmortyshowcase.core.domain.model.Character
import de.shinz.rickandmortyshowcase.core.domain.model.CharacterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The DAO against a real SQLite database, in memory.
 *
 * **In `iosTest` because it can only run there.** `Room.inMemoryDatabaseBuilder`
 * needs the bundled driver's native component, and `sqlite-bundled.aar` ships
 * only Android `.so`s — so an unmocked JVM host test has nothing to open. iOS is
 * part of every task's verification anyway, so the coverage is real rather than
 * theoretical.
 *
 * These are the cases a faked DAO cannot reach, and two of them were previously
 * asserted against the fake's own behaviour instead of SQLite's:
 * the `ORDER BY`, that a missing row is `null` rather than an error, and that
 * deleting an absent row is a no-op.
 */
class FavoriteCharacterDaoTest {

    @Test
    fun ordersFavouritesMostRecentlyAddedFirst() = runTest {
        // The claim the fake could never check: drop the ORDER BY, or point it at
        // createdEpochMillis, and nothing else in the suite notices.
        val dao = database().favoriteCharacterDao()
        dao.upsert(RICK.toFavoriteEntity(addedAt = EARLIER))
        dao.upsert(MORTY.toFavoriteEntity(addedAt = LATER))

        dao.observeAll().test {
            assertThat(awaitItem().map { it.id }).isEqualTo(listOf(MORTY.id, RICK.id))
        }
    }

    @Test
    fun aMissingRowIsNullRatherThanAnError() = runTest {
        assertThat(database().favoriteCharacterDao().getById(id = 9999)).isNull()
    }

    @Test
    fun deletingAnAbsentRowIsANoOp() = runTest {
        val dao = database().favoriteCharacterDao()

        dao.deleteById(id = 9999)

        dao.observeAll().test {
            assertThat(awaitItem()).isEqualTo(emptyList())
        }
    }

    @Test
    fun upsertReplacesAnExistingRowInsteadOfFailingOnThePrimaryKey() = runTest {
        val dao = database().favoriteCharacterDao()
        dao.upsert(MORTY.toFavoriteEntity(addedAt = EARLIER))

        dao.upsert(MORTY.copy(name = "Morty C-137").toFavoriteEntity(addedAt = LATER))

        dao.observeAll().test {
            val rows = awaitItem()
            assertThat(rows.size).isEqualTo(1)
            assertThat(rows.single().name).isEqualTo("Morty C-137")
        }
    }

    @Test
    fun aStoredCharacterComesBackIntactThroughRealSqlite() = runTest {
        // The offline guarantee, end to end: domain -> SQLite -> domain.
        val dao = database().favoriteCharacterDao()
        dao.upsert(MORTY.toFavoriteEntity(addedAt = LATER))

        assertThat(dao.getById(MORTY.id)?.toCharacter()).isEqualTo(MORTY)
    }

    @Test
    fun observeIdsReturnsOnlyTheIds() = runTest {
        val dao = database().favoriteCharacterDao()
        dao.upsert(RICK.toFavoriteEntity(addedAt = EARLIER))
        dao.upsert(MORTY.toFavoriteEntity(addedAt = LATER))

        dao.observeIds().test {
            assertThat(awaitItem().sorted()).isEqualTo(listOf(RICK.id, MORTY.id).sorted())
        }
    }

    private fun database(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    private companion object {
        val EARLIER = Instant.fromEpochMilliseconds(1_700_000_000_000L)
        val LATER = Instant.fromEpochMilliseconds(1_700_000_999_000L)

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
            created = Instant.parse("2017-11-04T18:50:21.651Z"),
        )

        val RICK = MORTY.copy(id = 1, name = "Rick Sanchez")
    }
}
