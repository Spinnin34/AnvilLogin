plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "net.islandearth"
version = "1.1.9"

repositories {
    mavenCentral()

    maven("https://erethon.de/repo/")
    maven("https://repo.convallyria.com/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    maven {
        name = "codemc-snapshots"
        url = uri("https://repo.codemc.io/repository/maven-snapshots/")
    }

    flatDir { dirs("libraries") } // FastLogin
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT")
    compileOnly(":FastLoginBukkit")

    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.1")
    implementation("net.wesjd:anvilgui:1.9.2-SNAPSHOT") // anvilgui
    implementation("com.convallyria.languagy:api:3.0.3-SNAPSHOT") {
        exclude("com.convallyria.languagy.libs")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")

        relocate("com.convallyria.languagy", "net.islandearth.anvillogin.libs.languagy")
        relocate("net.wesjd.anvilgui", "net.islandearth.anvillogin.libs.anvilgui")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
        options.release.set(11)
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}