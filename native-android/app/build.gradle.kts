import java.math.BigInteger
import java.util.Locale

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

val builtinIconDirectory = layout.projectDirectory.dir("src/main/assets/builtin-icons")
val supportedBuiltinIconExtensions = setOf(".png", ".webp", ".jpg", ".jpeg", ".svg")
val builtinIconOrderPrefix = Regex("^(\\d+)_(.+)$")

tasks.register("verifyBuiltinIconKeys") {
  group = "verification"
  description = "Fails when built-in icons have duplicate stable keys."
  inputs.dir(builtinIconDirectory)

  doLast {
    val duplicateKeys = builtinIconDirectory.asFile
      .listFiles()
      .orEmpty()
      .asSequence()
      .filter { it.isFile }
      .map { it.name }
      .filter { fileName ->
        supportedBuiltinIconExtensions.any { extension ->
          fileName.endsWith(extension, ignoreCase = true)
        }
      }
      .groupBy { fileName ->
        val prefixMatch = builtinIconOrderPrefix.matchEntire(fileName)
        val order = prefixMatch?.groupValues?.get(1)
        val keyCandidate = prefixMatch?.groupValues?.get(2)
        val hasValidPrefix = order != null &&
          keyCandidate != null &&
          supportedBuiltinIconExtensions.any { extension ->
            keyCandidate.endsWith(extension, ignoreCase = true)
          } &&
          runCatching { BigInteger(order) }.isSuccess
        (if (hasValidPrefix) keyCandidate.orEmpty() else fileName).lowercase(Locale.ROOT)
      }
      .filterValues { it.size > 1 }

    check(duplicateKeys.isEmpty()) {
      val conflicts = duplicateKeys.values.joinToString("; ") { files ->
        files.joinToString(", ")
      }
      "Duplicate built-in icon keys. Use a unique filename after the optional numeric prefix: $conflicts"
    }
  }
}

tasks.named("preBuild") {
  dependsOn("verifyBuiltinIconKeys")
}

android {
  namespace = "com.cgl.ifind"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.cgl.ifind"
    minSdk = 26
    targetSdk = 35
    versionCode = 12
    versionName = "2.3.7"

    vectorDrawables {
      useSupportLibrary = true
    }
  }

  splits {
    abi {
      isEnable = true
      reset()
      include("arm64-v8a")
      isUniversalApk = false
    }
  }

  signingConfigs {
    create("release") {
      storeFile = file("../../android/keystores/release.keystore")
      storePassword = System.getenv("IFIND_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("IFIND_KEY_ALIAS")
      keyPassword = System.getenv("IFIND_KEY_PASSWORD")
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }

    release {
      isMinifyEnabled = true
      isShrinkResources = true
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  buildFeatures {
    aidl = true
    buildConfig = true
    viewBinding = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  packaging {
    resources {
      excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1")
    }
  }
}

dependencies {
  implementation("androidx.activity:activity-ktx:1.10.0")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.recyclerview:recyclerview:1.4.0")
  implementation("com.caverock:androidsvg-aar:1.4")
  implementation("dev.rikka.shizuku:api:13.1.5")
  implementation("dev.rikka.shizuku:provider:13.1.5")
  testImplementation("junit:junit:4.13.2")
}
