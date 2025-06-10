// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        version.set("1.6.0")
        verbose.set(true)
        android.set(true)
        outputToConsole.set(true)
    }

    dependencies {
        add("ktlintRuleset", "io.nlopez.compose.rules:ktlint:0.4.22")
    }
}
