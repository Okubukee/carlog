import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.24" // O la versión más reciente/adecuada para Kotlin
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CarMaintenanceApp"
            packageVersion = "1.0.0"

            description = "Aplicación de Gestión de Mantenimiento de Coches"
            copyright = "© 2024 Car Maintenance App. All rights reserved."
            vendor = "Car Maintenance Solutions"

            windows {
                menuGroup = "Car Maintenance"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
            }

            macOS {
                // Use -Pcompose.desktop.mac.sign=true to sign and notarize.
                bundleID = "com.example.carmaintenance"
            }

            linux {
                packageName = "car-maintenance-app"
                debMaintainer = "maintenance@example.com"
                menuGroup = "Office"
                appRelease = "1"
                appCategory = "Office"
            }
        }
    }
}