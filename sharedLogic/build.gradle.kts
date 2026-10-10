import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.io.StringReader
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    compilerOptions {
        // Room's @ConstructedBy needs an `expect object` (AppDatabaseConstructor).
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }

    android {
       namespace = "dev.spaceapps.weather.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)

            implementation(libs.room3.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences.core)

            api(libs.androidx.lifecycle.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.kotlinx.coroutines.playServices)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.playServices.location)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.koin.test)
            implementation(libs.turbine)
        }
    }
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { kspConfiguration ->
        add(kspConfiguration, libs.room3.compiler)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// Secrets: environment variable wins (CI), otherwise read from the git-ignored secrets.properties.
val secrets = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("secrets.properties"))
        .asText.orNull
        ?.let { load(StringReader(it)) }
}

fun secret(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: secrets.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: throw GradleException(
            "Missing secret '$name'. Copy secrets.properties.example to secrets.properties " +
                "and fill it in, or set the $name environment variable."
        )

buildkonfig {
    packageName = "dev.spaceapps.weather"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "OPEN_WEATHER_API_KEY", secret("OPEN_WEATHER_API_KEY"))
    }
}
