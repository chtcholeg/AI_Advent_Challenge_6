# GigaChat Multiplatform Chat Application (Day 4 - Different Reasoning Modes)

A cross-platform chat application built with Kotlin Compose Multiplatform that integrates with GigaChat AI. The application runs on Android, Desktop (JVM), and Web (WasmJs).

## New in Day 4: Different Reasoning Modes

This version introduces two new powerful reasoning modes that enhance AI problem-solving capabilities:

### Step-by-Step Reasoning Mode
When enabled, the AI solves problems by breaking them down into clear, logical steps. Perfect for:
- Mathematical calculations
- Logical puzzles
- Analytical questions
- Complex decision-making
- Learning and understanding processes

### Expert Panel Discussion Mode
The AI simulates a panel of 3-4 diverse experts who discuss the topic from different perspectives and form a consensus. Perfect for:
- Getting multiple viewpoints on a topic
- Business decisions
- Technical architecture discussions
- Career advice
- Any topic requiring diverse expertise

## Architecture

This project follows the MVI (Model-View-Intent) architecture pattern:

- **Model**: Immutable data models representing the application state
- **View**: Compose UI components that render the state
- **Intent**: User actions that trigger state changes
- **Store**: Central state management that processes intents and updates state

### Tech Stack

- **Kotlin Multiplatform**: Shared code across platforms
- **Compose Multiplatform**: UI framework
- **Ktor**: HTTP client for API calls
- **Kotlinx Serialization**: JSON serialization/deserialization
- **Kotlinx Coroutines**: Asynchronous programming
- **Koin**: Dependency injection
- **GigaChat API**: AI chatbot backend

## Project Structure

```
AI_Advent_Challenge_6/
├── composeApp/
│   └── src/
│       ├── commonMain/          # Shared code
│       │   └── kotlin/
│       │       └── ru/chtcholeg/app/
│       │           ├── data/           # Data layer (API, models, repository)
│       │           ├── domain/         # Business logic (models, use cases)
│       │           ├── presentation/   # UI layer (MVI, components)
│       │           ├── di/             # Dependency injection
│       │           └── util/           # Utilities
│       ├── androidMain/         # Android-specific code
│       ├── desktopMain/         # Desktop-specific code
│       └── wasmJsMain/          # Web-specific code
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Prerequisites

Before you begin, ensure you have the following installed:

- **JDK 17 or higher**: Required for Kotlin and Gradle
- **Android Studio**: For Android development and building
- **IntelliJ IDEA** (optional): Recommended for multiplatform development
- **Android SDK**: For Android builds (can be installed via Android Studio)

## Getting GigaChat API Credentials

To use this application, you need GigaChat API credentials:

1. Visit [GigaChat Developer Portal](https://developers.sber.ru/portal/products/gigachat)
2. Sign up or log in to your account
3. Create a new application/project
4. Obtain your **Client ID** and **Client Secret**

## Setup Instructions

### 1. Clone or Download the Project

```bash
cd /Users/shchepilov/AndroidStudioProjects/AI_Advent_Challenge_6
```

### 2. Configure API Credentials

The application uses Gradle properties to manage API credentials securely.

1. Copy the template file:
   ```bash
   cp local.properties.template local.properties
   ```

2. Edit `local.properties` and add your GigaChat credentials:
   ```properties
   gigachat.clientId=your_actual_client_id
   gigachat.clientSecret=your_actual_client_secret
   ```

   **Important:** The `local.properties` file is automatically excluded from version control (listed in `.gitignore`), so your credentials will remain secure.

### 3. Build the Project

```bash
./gradlew build
```

The BuildKonfig plugin will generate configuration constants from your `local.properties` file during the build process. These constants are embedded into the application for all platforms (Android, Desktop, Web).

## Running the Application

### Android

#### Method 1: Command Line

```bash
# Install on connected device or emulator
./gradlew :composeApp:installDebug

# Run the app
adb shell am start -n ru.chtcholeg.app/.MainActivity
```

#### Method 2: Android Studio

1. Open the project in Android Studio
2. Ensure `local.properties` is configured with your credentials
3. Select an Android device or emulator
4. Click the **Run** button (or press `Shift + F10`)

### Desktop (JVM)

```bash
# Run the desktop application
./gradlew :composeApp:runDesktop
```

**Alternative:** Run from IntelliJ IDEA:
1. Open the project in IntelliJ IDEA
2. Ensure `local.properties` is configured with your credentials
3. Run the desktop main function (Shift + F10)

### Web (WasmJs)

```bash
# Start development server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

The application will open in your default browser at `http://localhost:8080`.

**Note:** All platforms use the same credentials from `local.properties`, which are embedded at build time via the BuildKonfig plugin.

## Building Distributable Packages

### Android APK

```bash
# Debug APK
./gradlew :composeApp:assembleDebug

# Release APK (requires signing configuration)
./gradlew :composeApp:assembleRelease

# Output: composeApp/build/outputs/apk/
```

### Desktop Installers

```bash
# Package for current OS
./gradlew :composeApp:packageDistributionForCurrentOS

# Output:
# - macOS: composeApp/build/compose/binaries/main/dmg/
# - Windows: composeApp/build/compose/binaries/main/msi/
# - Linux: composeApp/build/compose/binaries/main/deb/
```

### Web Build

```bash
# Production build
./gradlew :composeApp:wasmJsBrowserProductionWebpack

# Output: composeApp/build/dist/wasmJs/productionExecutable/
```

Deploy the contents of the output directory to your web server.

## Features

- Real-time chat with GigaChat AI and HuggingFace models
- **Step-by-Step Reasoning Mode** (NEW in Day 4) - AI solves problems step-by-step:
  - Breaks down complex problems into logical steps
  - Shows reasoning at each step
  - Provides clear final answers with explanation
  - Ideal for math, logic, and analytical questions
- **Expert Panel Discussion Mode** (NEW in Day 4) - Simulated expert discussion:
  - 3-4 diverse experts with unique perspectives
  - Realistic debate and discussion
  - Consensus-based conclusions
  - Multiple viewpoints on any topic
- **Dialog Mode** - Interactive requirement gathering through conversation:
  - AI asks clarifying questions one at a time
  - Builds context progressively through dialogue
  - Collects all necessary information before providing final result
  - Perfect for creating technical specifications, project plans, and requirements
- **Structured JSON Response Mode** - AI responds in strict JSON format with:
  - Question summary
  - Detailed response
  - Expert role identification
  - Relevant unicode symbols
  - Toggle between JSON and formatted view
- Cross-platform support (Android, Desktop, Web)
- Clean MVI architecture
- Conversation history management
- Error handling with retry functionality
- Loading indicators
- Message timestamps
- Clear chat functionality
- Configurable AI parameters (temperature, top-p, max tokens, repetition penalty)
- Multiple AI model support (GigaChat, Llama, DeepSeek)

## Troubleshooting

### Gradle Sync Fails

```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Credentials Not Found or Empty

**Symptom:** Empty credentials or build errors related to missing configuration

**Solution:**
- Ensure `local.properties` exists in the project root directory
- Verify the property names are correct: `gigachat.clientId` and `gigachat.clientSecret`
- Check that the values are not empty in `local.properties`
- Run `./gradlew clean` and rebuild the project
- If using Android Studio/IntelliJ, sync the Gradle project (File → Sync Project with Gradle Files)

### Android Build Fails

**Check:**
- Android SDK is installed and `ANDROID_HOME` is set
- Target SDK version matches your installed SDK
- Run `./gradlew :composeApp:dependencies` to check for conflicts

### Desktop Build Fails

**Check:**
- JDK version is 17 or higher: `java -version`
- `JAVA_HOME` is set correctly
- Try running with `--stacktrace` for more details

### Network Errors

**Symptom:** Connection errors when sending messages

**Check:**
- Internet connection is active
- GigaChat API is accessible
- API credentials are valid
- Check API rate limits

### Koin Injection Errors

**Symptom:** `NoBeanDefFoundException` or similar

**Solution:**
- Ensure `initKoin()` is called before creating composables
- Check that all dependencies are properly defined in Koin modules
- Verify platform-specific modules are created correctly

## Project Configuration

### Selecting Response Mode (Updated in Day 4)

The application now features five distinct response modes accessible through a dropdown menu in Settings:

1. Open Settings (gear icon)
2. Find "Response Mode" dropdown selector
3. Choose from five options:
   - **Normal Mode** (default): Standard conversational AI responses
   - **Structured JSON Response**: AI returns data in strict JSON format
   - **Dialog Mode**: AI asks clarifying questions to gather complete information
   - **Step-by-Step Reasoning** (NEW): AI solves problems step by step
   - **Expert Panel Discussion** (NEW): AI simulates expert panel discussion

**Response Mode Details:**

#### Normal Mode
- AI responds directly to your questions
- Conversational and natural interaction
- Best for general Q&A and casual conversation

#### Structured JSON Response Mode
- AI responds in strict JSON format with:
  - Question summary
  - Detailed response
  - Expert role
  - Unicode emoji symbols
- Click "Format" button to view beautifully formatted response
- Perfect for structured data extraction

#### Dialog Mode (Recommended for Requirements Gathering)
- AI asks clarifying questions **one at a time** (never multiple questions at once)
- Builds context progressively through dialogue
- Collects all necessary information before providing final result
- Perfect for:
  - Creating technical specifications
  - Generating project requirements
  - Planning system architecture
  - Drafting detailed documentation
  - Gathering business requirements

#### Step-by-Step Reasoning Mode (NEW in Day 4)
- AI breaks down problems into numbered steps
- Shows reasoning at each stage
- Provides clear structure:
  - Understanding the Problem
  - Given Information
  - Step-by-Step Solution
  - Final Answer/Conclusion
- Perfect for:
  - Mathematical problems
  - Logical puzzles
  - Analytical questions
  - Learning complex concepts

**Example Step-by-Step Workflow:**
```
User: "If a train travels 120 km in 2 hours, how long will it take to travel 300 km?"

AI:
**Understanding the Problem:**
We need to find the time required for a train to travel 300 km.

**Given Information:**
- Distance traveled: 120 km
- Time taken: 2 hours
- New distance to calculate: 300 km

**Step 1: Calculate the train's speed**
Speed = 120 km / 2 hours = 60 km/h

**Step 2: Calculate time for new distance**
Time = 300 km / 60 km/h = 5 hours

**Final Answer:**
The train will take **5 hours** to travel 300 km.
```

#### Expert Panel Discussion Mode (NEW in Day 4)
- AI simulates 3-4 diverse experts discussing the topic
- Each expert provides unique perspective based on their background
- Includes realistic debate with agreements and disagreements
- Forms consensus conclusion incorporating multiple viewpoints
- Perfect for:
  - Business decisions
  - Career advice
  - Technical architecture choices
  - Any topic requiring diverse expertise

**Example Expert Panel Workflow:**
```
User: "Should I learn Python or JavaScript first?"

AI:
**Panel Introduction:**
Today's panel features experts in software education:

**Expert 1 - Senior Software Engineer:** "Alex Chen"
"Python for beginners - clean syntax, great for learning concepts..."

**Expert 2 - Web Development Lead:** "Sarah Miller"
"JavaScript gives immediate visual feedback when building websites..."

**Expert 3 - Career Coach:** "Michael Thompson"
"The answer depends on your goals - JS for web, Python for data science..."

**Consensus Conclusion:**
Choose Python for data science/AI, JavaScript for web development...
```

**Important:** The response modes are mutually exclusive - only one can be active at a time. Switching modes will automatically update the chat interface.

### Changing AI Model

Go to Settings (⚙️ icon) and select from available models:
- GigaChat (Sberbank)
- Llama 3.2 3B Instruct (HuggingFace)
- Meta Llama 3 70B Instruct (HuggingFace)
- DeepSeek V3 (HuggingFace)

### Adjusting AI Parameters

Edit settings in the app UI:
- Temperature (0.0-2.0): Controls randomness
- Top P (0.0-1.0): Nucleus sampling threshold
- Max Tokens (1-8192): Response length limit
- Repetition Penalty (0.0-2.0): Reduces repetition

## Development

### Adding New Features

1. **Data Layer**: Add models, API methods, repository methods
2. **Domain Layer**: Create use cases for business logic
3. **Presentation Layer**: Define new intents and update state
4. **UI Layer**: Create composable components

### Running Tests

```bash
./gradlew test
```

### Code Style

This project follows the [Official Kotlin Code Style](https://kotlinlang.org/docs/coding-conventions.html).

## License

This project is created for educational purposes.

## Contact

For issues or questions, please refer to the GigaChat API documentation:
- [GigaChat API Docs](https://developers.sber.ru/docs/ru/gigachat/api/overview)

## Acknowledgments

- Built with [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- UI powered by [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- AI by [GigaChat](https://developers.sber.ru/portal/products/gigachat)

## Video
 
- https://disk.yandex.ru/i/3SbarrazdzrQYQ