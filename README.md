# Build Accent 🗣️

**Build Accent** is a modern Android application designed for accent and pronunciation training through **Active Mimicry** (Shadowing). By presenting text paired with high-quality reference audio, the app helps users master the rhythm, intonation, and stress of English (or any other language) in a disciplined, distraction-free environment.

---

## ✨ Features

- **The Learning Loop:** Enforced UX that encourages listening to the reference before recording and comparing versions side-by-side.
- **Synchronized Text:** Lessons feature timestamped segments. Tapping any text block instantly seeks the audio to that specific phoneme or sentence.
- **Interactive Studio:** Large, centered text display with professional audio controls, including an interactive seeking slider.
- **Dynamic Lesson Library:** Automatically imports lessons from the app's internal resources (`res/raw`). Simply drop in your `lesson_XX.opus` and `lesson_XX_text.txt`.
- **Category Navigation:** Intuitive tab-based language filtering. Prioritize your learning by setting a preferred language in settings.
- **Full Data Portability:** Export your entire progress—including database metadata and every single recording—into a portable ZIP backup.
- **Modern UI/UX:** Built with Jetpack Compose and Material 3. Supports **Dark Theme** and **Edge-to-Edge** rendering for an immersive experience.
- **Offline First:** No cloud dependency. Your voice, your data, stays on your device.

---

## 🛠️ The Shadowing Technique

The app is built around a proven scientific method for language acquisition:

1. **Listen:** Focus deeply on the native speaker's speed and emotion.
2. **Record:** Mimic the audio exactly as you heard it.
3. **Compare:** Open "Audio Records" to listen to your take against the reference.
4. **Refine:** Use the clickable text segments to pinpoint and repeat difficult sections until you sound identical to the source.

---

## 📦 Technical Stack

- **UI:** Jetpack Compose (Material 3)
- **Language:** Kotlin (with reserved keyword handling for the `com.buildaccent.as` package)
- **Database:** Room Persistence Library
- **Preferences:** Jetpack DataStore (Preferences)
- **Audio:** MediaPlayer & MediaRecorder APIs
- **JSON Serialization:** Gson
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern

---

## 🚀 Getting Started

### Prerequisites
- Android device running API 24 (Nougat) or higher.
- Java 17+ for building.

### Build Instructions
1. Clone the repository.
2. Create a `keystore.properties` file in the root directory:
   ```properties
   storePassword=your_password
   keyPassword=your_password
   keyAlias=your_alias
   storeFile=your_keystore.jks
   ```
3. Run the Gradle build:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 💾 Data Management

- **Backup:** Generates a ZIP file containing `metadata.json` and an `audio/` directory.
- **Restore:** Uses a "Smart Append" strategy. Importing a backup adds data to your library without overwriting or deleting your current progress.

---

## 👨‍💻 Developer

**Ahmmed Soyeb**  
[GitHub Profile](https://github.com/estiaksoyeb)

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for the full text.