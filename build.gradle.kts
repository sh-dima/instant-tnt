import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)

    alias(libs.plugins.paper)
    alias(libs.plugins.paper.run)

    alias(libs.plugins.yml)
}

repositories {
    mavenCentral()
}

dependencies {
    library(kotlin("stdlib"))
    library(libs.config)
    implementation(libs.metrics)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform)

    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

group = "io.gitlab.shdima"

version = ProcessBuilder("git", "describe", "--tags", "--always", "--dirty")
    .directory(project.projectDir)
    .start()
    .inputStream
    .bufferedReader()
    .readText()
    .trim()

tasks {
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        filePermissions {
            user.read = true
            user.write = true
            user.execute = false

            group.read = true
            group.write = false
            group.execute = false

            other.read = true
            other.write = false
            other.execute = false
        }

        dirPermissions {
            user.read = true
            user.write = true
            user.execute = true

            group.read = true
            group.write = false
            group.execute = true

            other.read = false
            other.write = false
            other.execute = true
        }
    }

    runServer {
        val version = project.findProperty("minecraft.version") as? String ?: "1.21.8"
        val metricsEnabled = (project.findProperty("metrics") as? String)?.toBoolean() == true

        minecraftVersion(version)

        doFirst {
            val metricsConfig = runDirectory.get().asFile.resolve("plugins/bStats/config.yml")

            metricsConfig.parentFile.mkdirs()
            metricsConfig.writeText("""
                enabled: $metricsEnabled
                logFailedRequests: true

            """.trimIndent())
        }
    }

    withType<ShadowJar> {
        from("assets/text/licenses") {
            into("licenses")
        }

        archiveClassifier = ""

        enableAutoRelocation = true
        relocationPrefix = "${project.group}.${project.name}.dependencies"

        minimizeJar = true
    }

    jar {
        enabled = false
    }
}

listOf(
    tasks.shadowJar,
    tasks.kotlinSourcesJar,
).forEach {
    it {
        from("README.md")
        from("LICENSE")
    }
}

bukkit {
    name = "ChainTNT"
    description = "A Minecraft plugin that adds TNT which explodes instantly and in a chain, inspired by Bad Piggies."

    main = "$group.${project.name}.InstantTnt"
    apiVersion = "1.21.1"
    version = project.version.toString()

    authors = listOf(
        "Esoteric Enderman"
    )

    website = "https://gitlab.com/-/p/75772128"
}
