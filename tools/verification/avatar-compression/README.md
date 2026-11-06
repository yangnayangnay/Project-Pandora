# Avatar JPEG regression checks

These six JUnit checks use Java ImageIO to exercise the production `AvatarJpegCompressor` against real JPEG bytes. Android's compilation API does not include `java.awt` or `javax.imageio`, so these tests compile separately against the application's compiled Java classes and the project's cached JUnit 4.13.2 / Hamcrest dependencies. They add no runtime dependencies to the app.

The local runner `.build-temp/verify-avatar-compression.ps1` first checks the Gradle task graph, compiles app and instrumentation test sources, and runs the existing Android JVM suite. It then compiles this file using the existing JDK with `--release 11` and runs `org.junit.runner.JUnitCore` in headless mode. Outputs go under `.build-temp/avatar-compression-jvm/`; this does not generate or install an APK.

Android's actual Bitmap adapter, original files larger than 5MB, and EXIF handling are covered by `AvatarHelperTest` instrumentation sources. Those sources are compiled only; running them on a device requires separate authorization to build/install APKs.
