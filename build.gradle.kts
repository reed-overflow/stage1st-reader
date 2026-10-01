import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.SignPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    java
    // 2.14+ requires IDEA 2023.3; 2.13.1 still supports the 223 baseline.
    id("org.jetbrains.intellij.platform") version "2.13.1"
    id("org.jetbrains.changelog") version "2.5.0"
    id("org.jetbrains.qodana") version "0.1.13"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        testFramework(TestFrameworkType.Platform)
        pluginVerifier(providers.gradleProperty("pluginVerifierVersion"))
        zipSigner(providers.gradleProperty("signingCliVersion"))
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set(providers.gradleProperty("pluginName"))
        version.set(providers.gradleProperty("pluginVersion"))
        ideaVersion {
            sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
            untilBuild.set(provider { null })
        }
        description.set(providers.fileContents(layout.projectDirectory.file("README.md")).asText.map { readme ->
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"
            require(start in readme && end in readme) { "Plugin description section not found in README.md" }
            markdownToHTML(readme.substringAfter(start).substringBefore(end).trim())
        })
        changeNotes.set(provider {
            changelog.run {
                renderItem(getOrNull(version.get()) ?: getUnreleased(), Changelog.OutputType.HTML)
            }
        })
    }
    pluginVerification {
        ides {
            local(provider { intellijPlatform.platformPath.toFile() })
            create(providers.gradleProperty("pluginVerifierIdeVersions").map { it.split(',').map(String::trim) })
        }
        failureLevel.set(listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
        ))
    }
    signing {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }
    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(providers.gradleProperty("pluginVersion").map {
            listOf(it.substringAfter('-', "default").substringBefore('.'))
        })
    }
}

changelog {
    version.set(providers.gradleProperty("pluginVersion"))
    groups.set(emptyList())
}

qodana {
    cachePath.set(layout.projectDirectory.dir(".qodana").asFile.path)
    reportPath.set(layout.buildDirectory.dir("reports/inspections").get().asFile.path)
    saveReport.set(true)
    showReport.set(providers.environmentVariable("QODANA_SHOW_REPORT").map(String::toBoolean).orElse(false))
}

intellijPlatformTesting.runIde.register("runIdeForUiTests") {
    task {
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        })
    }
    plugins {
        robotServerPlugin()
    }
}

tasks {
    assemble {
        dependsOn(buildPlugin)
    }
    check {
        dependsOn(verifyPluginProjectConfiguration, verifyPluginStructure)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
        networkTimeout.set(60_000)
    }

    val validateSigning = register("validateSigning") {
        group = "publishing"
        description = "Validate signing credentials before creating a signed distribution."
        doLast {
            val missing = listOf("CERTIFICATE_CHAIN", "PRIVATE_KEY", "PRIVATE_KEY_PASSWORD")
                .filter { System.getenv(it).isNullOrBlank() }
            check(missing.isEmpty()) { "Missing signing environment variables: " + missing.joinToString() }
        }
    }
    val releaseVersion = providers.gradleProperty("pluginVersion")
    val validatePublishing = register("validatePublishing") {
        group = "publishing"
        description = "Validate the Marketplace token and release version."
        doLast {
            check(!System.getenv("PUBLISH_TOKEN").isNullOrBlank()) { "Missing PUBLISH_TOKEN. See docs/publishing.md." }
            val value = releaseVersion.get()
            val pattern = Regex("""(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[a-z][a-z0-9]*(?:\.(?:0|[1-9][0-9]*))?)?""")
            check(pattern.matches(value) && value.substringAfter("-", "").substringBefore(".") != "default") {
                "pluginVersion must be X.Y.Z or X.Y.Z-channel[.N]."
            }
        }
    }
    signPlugin {
        dependsOn(validateSigning)
    }
    publishPlugin {
        dependsOn(validatePublishing, "check", "verifyPlugin")
        // Always publish the signed ZIP, including when signing is UP-TO-DATE.
        archiveFile.set(named<SignPluginTask>("signPlugin").flatMap { it.signedArchiveFile })
    }
}
