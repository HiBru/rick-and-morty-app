package de.shinz.rickandmortyshowcase.features.favorites.presentation.model

/**
 * One field, and that is the whole screen's own state.
 *
 * There is no paging buffer and no load state: the database is observed, not
 * requested, so there is nothing to retry and nothing to accumulate.
 */
data class FavoritesViewModelState(
    /**
     * The character a confirmation dialog is open for, or `null`.
     *
     * An id, resolved against the list by the assembler. That resolution can
     * fail, and the assembler handles it — but **not because the app can
     * currently produce it**: the dialog is modal, so nothing else can remove
     * the character while it is open, and confirming clears this first. The
     * branch exists because an assembler is a pure function of its inputs and
     * has to answer for all of them, which is the same reason the detail
     * screen's assembler pins pairings its ViewModel never emits.
     */
    val pendingRemovalId: Int? = null,
)
