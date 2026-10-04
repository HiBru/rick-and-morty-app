# App Specification

Screen behaviour and flows. Architecture, naming, and stack decisions live in [CLAUDE.md](../CLAUDE.md) — this file deliberately names no classes.

All user-facing copy is **English**.

## Navigation shell

A **Dashboard** screen hosts a `BottomNavigationBar` with exactly three items:

| Item | Content |
|---|---|
| Home | Paginated list of all Rick and Morty characters |
| Favorites | Locally saved characters |
| Settings | App theme selection |

**Character detail is a top-level destination above the Dashboard**, not inside a tab. It covers the bottom bar, and it is the same destination whether it was opened from Home or from Favorites.

Switching tabs preserves each tab's scroll position and state.

## Home

A simple list screen. Characters come from `GET /character?page=N` — **20 per page**. The next page loads when the user approaches the end of the list and is appended; paging stops when the API reports no next page (`info.next` is `null`).

Each row shows:

- character image
- name
- species
- status

A **favorite icon** on each row toggles that character's favorite state.

Tapping a row opens the detail screen, passing **only the character id**.

**States:** a loading indicator for the first page; a distinct inline indicator for subsequent pages; an error state with a retry affordance when a page fails. A failed page load never discards pages already shown.

## Favorites

Lists every character stored in the local database, **using the same row design as Home**.

- When there are no favorites, show an **empty state** instead of the list.
- Tapping a row opens the same detail screen as Home, again passing only the character id.
- Favorites can be removed from here.

This screen reads only from the local database — it never calls the API, so it works fully offline.

## Detail

Receives **only the character id**. It then resolves the data itself:

1. Look for a local entry with that id.
2. If one exists, render the **local** data.
3. Otherwise load the character from the **remote API**.

This means a favorited character opens without a network call, and the screen works offline for anything saved.

Shows the character's full information:

- image, name, status, species
- type (only when the API provides a non-empty value)
- gender
- origin name
- last known location name
- number of episodes the character appears in
- created date

Resolving episode *names* is out of scope — it would require one request per episode.

The detail screen offers the **same favorite toggle** as the lists.

**States:** loading while resolving; an error state with retry when the remote load fails and no local entry exists.

## Settings

Exactly one setting: **app theme**, selectable as **Dark**, **Light**, or **System**. The choice persists across app restarts and applies immediately.

## Cross-cutting rules

**Favoriting persists every character value returned by the API** — all scalar fields, the origin and location names and urls, and the episode url list. This is what lets a favorited character render completely while offline.

**Removing a favorite always goes through a confirmation dialog.** This holds everywhere the removal is possible: the Home list, the Favorites list, and the detail screen. **Adding** a favorite is immediate and needs no confirmation.

Favorite state is consistent across screens — favoriting on Home is visible on the detail screen and in Favorites without a manual refresh, and removing in Favorites updates Home.
