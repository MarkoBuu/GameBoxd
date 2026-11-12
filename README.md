# GameBoxd 

**GameBoxd** is a modern, feature-rich Android application designed as a "Letterboxd for video games". It allows gamers to discover new titles, track their gaming library, log plays, write reviews, rate games, and view insightful statistics about their gaming habits.

---

## Key Features

- **Explore & Discover**: Browse trending, popular, and top-rated games powered by the **IGDB (Internet Game Database) API**. Search across thousands of titles with advanced filtering and sorting options.
- **Personal Game Library**: Organize your collection into various lists:
  - Currently Playing
  - Completed
  - Backlog / Plan to Play
  - Wishlist
  - Dropped & Custom Statuses
- **Detailed Game Views**: View high-resolution cover art, screenshots, release dates, genres, developer info, summaries, and community/critic ratings.
- **Logging & Reviews**: Log your gameplay sessions, record personal ratings (with star ratings), write reviews, and track play status.
- **Shareable Review Cards**: Generate beautiful, shareable review summary cards for your favorite games.
- **Gaming Statistics**: Analyze your gaming library with built-in statistics, completion rates, rating distributions, and genre breakdowns.
- **Offline Persistence**: Seamlessly cache and store your library and preferences locally using **Room Database**.

---

## Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/) design principles.
- **Architecture**: **MVVM (Model-View-ViewModel)** with Repository pattern and Kotlin Coroutines/Flow for reactive state management.
- **Dependency Injection**: [Koin](https://insert-koin.io/) for lightweight, pragmatic dependency injection.
- **Networking**: [Ktor Client](https://ktor.io/) (OkHttp engine) with Kotlinx Serialization for fast, type-safe API communication with IGDB.
- **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) for robust offline data storage.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for efficient asynchronous image loading and caching.

---

## Project Structure

```text
com.example.gameboxd/
├── data/
│   ├── local/         # Room database entities, DAOs, and local helpers
│   ├── model/         # Domain and data models (Game, Filters, Sort options)
│   ├── remote/        # IGDB API integration via Ktor & queries
│   └── repository/    # Single source of truth repositories
├── di/                # Koin dependency injection modules
└── ui/
    ├── components/    # Reusable Compose components (GameCards, RatingStars, etc.)
    ├── navigation/    # Navigation graph and screen routes
    ├── screens/       # Main app screens (Explore, Library, Stats, Detail Dialog)
    ├── theme/         # Material 3 theme, colors, and typography
    └── utils/         # Utilities (Review card generator, helpers)
```

---

## Setup & Configuration

GameBoxd integrates with the **IGDB API** (via Twitch Authentication). To run the app with live data from IGDB, you need to configure your Twitch Developer credentials.

1. Obtain a **Client ID** and **Client Secret** from the [Twitch Developer Console](https://dev.twitch.tv/).
2. Create or update a `local.properties` file in the root directory of the project (`/Users/mbudak/StudioProjects/GameBoxd/local.properties`) and add your credentials:

   ```properties
   TWITCH_CLIENT_ID=your_twitch_client_id_here
   TWITCH_CLIENT_SECRET=your_twitch_client_secret_here
   ```

3. Sync Gradle and run the app from Android Studio or via ADB.

---

## Building & Running

You can build and test the project directly using Gradle:

- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew test
  ```

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.
