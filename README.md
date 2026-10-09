This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/sharedLogic](./sharedLogic/src) is for the code that will be shared between app targets in the project.
  The most important subfolder is [commonMain](./sharedLogic/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/sharedUI](./sharedUI/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./sharedUI/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./sharedUI/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./sharedUI/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Secrets

Copy `secrets.properties.example` to `secrets.properties` (git-ignored) and set `OPEN_WEATHER_API_KEY`.
An environment variable with the same name takes precedence.

### CI/CD (GitHub Actions)

- **CI** ([ci.yml](.github/workflows/ci.yml)) runs on every pull request and push to `main`: shared tests on the JVM and
  the iOS simulator, Android lint and debug build, and an iOS simulator build. It needs no secrets.
- **Release** ([release.yml](.github/workflows/release.yml)) runs when a `v*` tag is pushed (`git tag v1.2.0 && git push origin v1.2.0`).
  It builds a signed Android APK and an ad hoc signed iOS IPA and sends both to Firebase App Distribution.
  The tag sets the version name (`1.2.0`) and the workflow run number sets the build number.

Both release jobs use the `release` environment, so secrets can live there and you can add required reviewers to it.

| Secret | Contents |
|---|---|
| `OPEN_WEATHER_API_KEY` | OpenWeather API key |
| `ANDROID_KEYSTORE_BASE64` | Release keystore: `base64 -w0 release.jks` |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | Keystore credentials |
| `IOS_CERTIFICATE_P12_BASE64` | Apple Distribution certificate with private key, exported as .p12: `base64 -i cert.p12` |
| `IOS_CERTIFICATE_PASSWORD` | Password of the .p12 |
| `IOS_PROVISIONING_PROFILE_BASE64` | Ad Hoc provisioning profile for the bundle ID, including tester device UDIDs |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Service account key JSON with the *Firebase App Distribution Admin* role |

| Variable | Contents |
|---|---|
| `IOS_TEAM_ID` | Apple Developer team ID |
| `IOS_BUNDLE_ID` | iOS bundle ID matching the provisioning profile |
| `FIREBASE_ANDROID_APP_ID`, `FIREBASE_IOS_APP_ID` | Firebase app IDs (`1:1234567890:android:…`) |
| `FIREBASE_TESTER_GROUPS` | Optional, comma-separated tester group aliases (default `testers`) |

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…