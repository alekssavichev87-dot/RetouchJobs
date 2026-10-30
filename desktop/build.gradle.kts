import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.compose") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.example.retouchjobs.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "RetouchJobs"
            packageVersion = "1.0.0"
            description = "Job search app powered by Serper.dev"
            vendor = "alekssavichev87-dot"
            includeAllModules = true

            windows {
                iconFile.set(project.file("icons/app.ico"))
                menuGroup = "RetouchJobs"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "42bb35a0-cff0-47e2-a242-3420212a9130"
            }
        }
    }
}
