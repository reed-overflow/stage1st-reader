import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.intellij.tasks.SignPluginTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.4.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"

}

// IntelliJ Gradle Plugin 1.4 uses an obsolete, unversioned GitHub signer asset URL.
// Resolve the official executable JAR from Maven Central instead; do not bundle it with the plugin.
val marketplaceSigner by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    testImplementation("junit:junit:4.13.2")
    add(marketplaceSigner.name, "org.jetbrains:marketplace-zip-signer-cli:" + properties("signingCliVersion"))
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

tasks {
    val validateSigning by registering {
        group = "publishing"
        description = "Fail before signing when required credentials are missing."
        doLast {
            val missing = listOf("CERTIFICATE_CHAIN", "PRIVATE_KEY", "PRIVATE_KEY_PASSWORD")
                .filter { System.getenv(it).isNullOrBlank() }
            if (missing.isNotEmpty()) {
                throw GradleException("Missing signing environment variables: " + missing.joinToString())
            }
        }
    }

    val validatePublishing by registering {
        group = "publishing"
        description = "Validate the Marketplace token and release version."
        doLast {
            if (System.getenv("PUBLISH_TOKEN").isNullOrBlank()) {
                throw GradleException("Missing PUBLISH_TOKEN. See docs/publishing.md.")
            }
            val releaseVersion = properties("pluginVersion")
            val releasePattern = Regex("""(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[a-z][a-z0-9]*(?:\.(?:0|[1-9][0-9]*))?)?""")
            if (!releasePattern.matches(releaseVersion)
                || releaseVersion.substringAfter("-", "").substringBefore(".") == "default") {
                throw GradleException("pluginVersion must be X.Y.Z or X.Y.Z-channel[.N].")
            }
        }
    }

    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Prefer this version's notes; let the plugin resolve its configured unreleased heading.
        changeNotes.set(provider {
            changelog.run {
                getOrNull(properties("pluginVersion")) ?: getUnreleased()
            }.toHTML()
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        dependsOn(validateSigning)
        cliVersion.set(properties("signingCliVersion"))
        cliPath.set(provider { marketplaceSigner.singleFile.absolutePath })
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn(validatePublishing, "check", "runPluginVerifier")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // 1.x otherwise falls back to the unsigned ZIP when signPlugin is UP-TO-DATE.
        // Always upload the exact signed file, including when recovering a failed upload.
        distributionFile.set(project.tasks.named<SignPluginTask>("signPlugin").flatMap { it.outputArchiveFile })
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    buildPlugin {
        archiveBaseName.set(properties("pluginName"))
        archiveVersion.set(properties("pluginVersion"))
    }

    runPluginVerifier {
        // Explicit endpoints of the supported IDE range; avoid an unbounded IDE download matrix.
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
        failureLevel.set(listOf(
            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
            RunPluginVerifierTask.FailureLevel.MISSING_DEPENDENCIES
        ))
    }

    compileJava {
        options.encoding = "UTF-8"
    }
}
