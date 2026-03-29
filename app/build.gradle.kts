import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

fun loadDotEnv(path: java.io.File): Properties {
    val props = Properties()
    if (!path.exists()) return props

    path.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachLine
        val idx = line.indexOf('=')
        if (idx <= 0) return@forEachLine

        val key = line.substring(0, idx).trim()
        var value = line.substring(idx + 1).trim()
        if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\''))) {
            value = value.substring(1, value.length - 1)
        }
        if (key.isNotEmpty()) props.setProperty(key, value)
    }
    return props
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val dotEnv = loadDotEnv(rootProject.file(".env"))

fun secret(name: String): String? =
    System.getenv(name)
        ?: dotEnv.getProperty(name)
        ?: localProperties.getProperty(name)

val googleServicesTemplate = project.file("google-services.template.json")
val googleServicesOutput = project.file("google-services.json")

val generateGoogleServicesJson = tasks.register("generateGoogleServicesJson") {
    group = "build setup"
    description = "Generates app/google-services.json from google-services.template.json using env/.env/local.properties."

    inputs.file(googleServicesTemplate)
    outputs.file(googleServicesOutput)

    doLast {
        if (!googleServicesTemplate.exists()) {
            throw GradleException("Missing ${googleServicesTemplate.path}. Expected a template file.")
        }

        val apiKey = secret("GOOGLE_SERVICES_CURRENT_KEY")
        if (apiKey.isNullOrBlank()) {
            throw GradleException(
                "Missing GOOGLE_SERVICES_CURRENT_KEY. Set it in environment variables, .env (repo root), or local.properties."
            )
        }

        val templateText = googleServicesTemplate.readText(Charsets.UTF_8)
        val rendered = templateText.replace("__GOOGLE_SERVICES_CURRENT_KEY__", apiKey)

        if (rendered.contains("__GOOGLE_SERVICES_CURRENT_KEY__")) {
            throw GradleException("Failed to render google-services.json: placeholder was not replaced.")
        }

        googleServicesOutput.writeText(rendered, Charsets.UTF_8)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateGoogleServicesJson)
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    dependsOn(generateGoogleServicesJson)
}

android {
    namespace = "com.example.tutorHub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tutorHub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseStoreFilePath = secret("RELEASE_STORE_FILE")
    val releaseStorePassword = secret("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = secret("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD")

    signingConfigs {
        if (
            !releaseStoreFilePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        } else {
            logger.warn(
                "Release signing is not configured. " +
                    "Set RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD " +
                    "in environment variables, .env, or local.properties."
            )
        }
    }

    buildTypes {
        getByName("release") {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.firebase.bom.v3340))
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.google.firebase.storage.ktx)
    implementation(libs.google.firebase.functions.ktx)

    implementation(platform(libs.androidx.compose.bom.v20241000))
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)


    // Image loading for previews
    implementation(libs.coil.compose)
}