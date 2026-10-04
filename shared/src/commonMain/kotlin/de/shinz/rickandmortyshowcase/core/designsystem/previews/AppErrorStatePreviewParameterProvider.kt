package de.shinz.rickandmortyshowcase.core.designsystem.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * Two data parameters, so they bundle.
 *
 * `message` is the one the skill counts — `onRetry` is a lambda and does not —
 * but the retryable look is a genuinely different component, so it travels with
 * it. The no-retry variant matters: a page `NOT_FOUND` means end-of-list, and
 * offering "Retry" there would be a lie.
 */
internal data class AppErrorStatePreviewData(
    val message: String,
    val retryable: Boolean,
)

internal class AppErrorStatePreviewParameterProvider :
    PreviewParameterProvider<AppErrorStatePreviewData> {

    override val values: Sequence<AppErrorStatePreviewData> = sequenceOf(
        AppErrorStatePreviewData(
            message = "No internet connection. Check your connection and try again.",
            retryable = true,
        ),
        AppErrorStatePreviewData(
            message = "The server is having trouble. Try again later. " +
                "If this keeps happening there is nothing you can do about it, " +
                "which is what a long message looks like here.",
            retryable = true,
        ),
        AppErrorStatePreviewData(
            message = "We couldn't find what you were looking for.",
            retryable = false,
        ),
    )

    override fun getDisplayName(index: Int): String =
        listOf("retryable", "long message", "not retryable")[index]
}
