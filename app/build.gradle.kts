// Configuração de build do MÓDULO "app" (o aplicativo em si).
// Este arquivo usa a sintaxe Kotlin do Gradle (.kts), padrão do Android Studio.
// Não é código do app: o app é 100% Java (pasta src/main/java).

plugins {
    // Plugin que sabe transformar o projeto num app Android (.apk). Não usamos o plugin de Kotlin.
    id("com.android.application")
}

android {
    namespace = "com.guard"   // pacote base das classes e da classe R
    compileSdk = 35           // versão do Android usada para COMPILAR

    defaultConfig {
        applicationId = "com.guard"  // identificador único do app no celular / na Play Store
        minSdk = 26                  // Android mínimo suportado (8.0)
        targetSdk = 35               // Android para o qual o app foi testado
        versionCode = 1              // número interno da versão (sobe a cada publicação)
        versionName = "0.1.0"        // versão mostrada para o usuário

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false   // não "encolhe" o código (desnecessário num projeto acadêmico)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Versão do Java usada para compilar o código do app.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Bibliotecas externas que o app usa (o Gradle baixa sozinho).
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")               // AppCompatActivity
    implementation("com.google.android.material:material:1.12.0")     // botões, chips, barras (Material Design)
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    testImplementation("junit:junit:4.13.2")                           // testes em app/src/test
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
