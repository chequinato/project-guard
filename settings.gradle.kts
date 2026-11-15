// Configuração do PROJETO inteiro: onde baixar plugins e bibliotecas, e quais módulos existem.

// Onde o Gradle procura PLUGINS (google() = repositório do Android)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Onde o Gradle procura BIBLIOTECAS (dependências)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Guard"
include(":app")   // o único módulo: a pasta app/
