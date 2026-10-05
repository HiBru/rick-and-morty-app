package de.shinz.rickandmortyshowcase.features.settings.presentation.model

/**
 * Deliberately empty.
 *
 * This screen has no form field, no dialog and no in-flight flag: the only thing
 * it shows is a preference it observes, and the only thing it does is write one.
 * The type is still here because the assembler's signature is
 * `assemble(data, vmState)` throughout the app — a settings screen that broke
 * that pattern to save one empty class would cost more than it saved.
 */
class SettingsViewModelState
