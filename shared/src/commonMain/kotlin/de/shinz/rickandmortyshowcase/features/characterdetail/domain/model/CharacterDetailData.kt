package de.shinz.rickandmortyshowcase.features.characterdetail.domain.model

/**
 * The one thing the detail screen observes rather than loads.
 *
 * The character itself is a **one-shot** read — the screen is handed an id and
 * resolves it once — so it lives in `CharacterDetailViewModelState`. The
 * favourite flag has to be a `Flow`, because `SPEC.md` requires favouriting here
 * to show up on the lists and vice versa without a refresh.
 *
 * A `Boolean` rather than the whole id set: this screen knows exactly one
 * character, so membership is resolved before it gets here and the assembler is
 * spared a lookup it could get wrong.
 */
data class CharacterDetailData(
    val isFavorite: Boolean,
) {
    companion object {
        val EMPTY = CharacterDetailData(isFavorite = false)
    }
}
