buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
        classpath("org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
}
