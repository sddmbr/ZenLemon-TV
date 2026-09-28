pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        maven { url = uri("https://repo.maven.apache.org/maven2/") }
        maven { url = uri("https://repo1.maven.org/maven2") }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ZenLemon"

include(":app")
include(":domain")
include(":data")
include(":player")
include(":plugins:squeeze")