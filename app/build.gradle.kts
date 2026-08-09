import java.util.Properties
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.jvm.JvmLibrary
import org.gradle.language.base.artifact.SourcesArtifact

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.isFile) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}
val releaseSigningConfigured = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword",
).all { !releaseSigningProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.tidefetch.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tidefetch.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.0-alpha.2"

        vectorDrawables {
            useSupportLibrary = true
        }

    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            // The upstream AAR still carries legacy x86; TideFetch targets the
            // three actively supported device/emulator ABIs below.
            excludes += setOf("**/x86/**")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    testImplementation("junit:junit:4.13.2")
}

tasks.register("exportReleaseDependencySources") {
    group = "compliance"
    description = "Exports source artifacts for the resolved release dependency graph."

    doLast {
        val output = rootProject.layout.projectDirectory.dir("compliance/cache/gradle-sources").asFile
        output.mkdirs()

        val componentIds = configurations.getByName("releaseRuntimeClasspath")
            .incoming.resolutionResult.allComponents
            .mapNotNull { it.id as? ModuleComponentIdentifier }
            .distinct()
        val resolution = dependencies.createArtifactResolutionQuery()
            .forComponents(componentIds)
            .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
            .execute()

        val manifest = mutableListOf<String>()
        resolution.resolvedComponents.sortedBy { it.id.displayName }.forEach { component ->
            val coordinate = component.id.displayName
            val artifacts = component.getArtifacts(SourcesArtifact::class.java)
                .filterIsInstance<ResolvedArtifactResult>()
            if (artifacts.isEmpty()) {
                manifest += "$coordinate\tNO_SOURCE_ARTIFACT"
            } else {
                artifacts.forEachIndexed { index, artifact ->
                    val safeCoordinate = coordinate.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val suffix = if (artifacts.size == 1) "" else "-$index"
                    val target = output.resolve("$safeCoordinate$suffix-sources.jar")
                    artifact.file.copyTo(target, overwrite = true)
                    manifest += "$coordinate\t${target.name}"
                }
            }
        }
        output.resolve("ARTIFACTS.tsv").writeText(manifest.joinToString("\n", postfix = "\n"))
    }
}
