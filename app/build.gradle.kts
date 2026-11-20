// Configuração de build do MÓDULO "app" (o aplicativo em si).
// Este arquivo usa a sintaxe Kotlin do Gradle (.kts), padrão do Android Studio.
// Não é código do app: o app é 100% Java (pasta src/main/java).

import java.util.Properties

plugins {
    // Plugin que sabe transformar o projeto num app Android (.apk). Não usamos o plugin de Kotlin.
    id("com.android.application")
}

// Lê o arquivo local.properties (fica só no seu PC: está no .gitignore e NÃO vai para o GitHub).
// É ali que colocamos os telefones REAIS dos contatos da demo, porque o repositório é público.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.reader(Charsets.UTF_8).use { load(it) }
}

// Pega uma chave do local.properties; se não existir, usa o valor de reserva (fallback).
fun local(key: String, fallback: String): String = localProps.getProperty(key, fallback).trim()

android {
    namespace = "com.guard"   // pacote base das classes e da classe R
    compileSdk = 35           // versão do Android usada para COMPILAR

    defaultConfig {
        applicationId = "com.guard"  // identificador único do app no celular / na Play Store
        minSdk = 26                  // Android mínimo suportado (8.0)
        targetSdk = 35               // Android para o qual o app foi testado
        versionCode = 2              // número interno da versão (sobe a cada publicação)
        versionName = "0.2.0"        // versão mostrada para o usuário

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Nome de quem usa o app (vai no SMS: "<nome> pode ter sofrido um acidente").
        // Também dá para mudar no app, na tela de contatos. Vazio = "Um usuário do Guard".
        buildConfigField("String", "USER_NAME", "\"${local("guard.user.name", "")}\"")
        // Contatos que o app já cadastra na primeira abertura (GuardDatabaseHelper.seedDemoContacts).
        // buildConfigField cria uma constante na classe BuildConfig: BuildConfig.CONTACT1_PHONE etc.
        // Sem local.properties, usamos números INVÁLIDOS (DDD 00) para nenhum estranho receber SMS.
        buildConfigField("String", "CONTACT1_NAME", "\"${local("guard.contact1.name", "Ana")}\"")
        buildConfigField("String", "CONTACT1_RELATIONSHIP", "\"${local("guard.contact1.relationship", "Team member")}\"")
        buildConfigField("String", "CONTACT1_PHONE", "\"${local("guard.contact1.phone", "+5500000000001")}\"")
        buildConfigField("String", "CONTACT2_NAME", "\"${local("guard.contact2.name", "Professor")}\"")
        buildConfigField("String", "CONTACT2_RELATIONSHIP", "\"${local("guard.contact2.relationship", "Professor")}\"")
        buildConfigField("String", "CONTACT2_PHONE", "\"${local("guard.contact2.phone", "+5500000000002")}\"")
        // 3º contato: só para mostrar o botão de excluir na apresentação. Número inválido de propósito.
        buildConfigField("String", "CONTACT3_NAME", "\"${local("guard.contact3.name", "Bruno")}\"")
        buildConfigField("String", "CONTACT3_RELATIONSHIP", "\"${local("guard.contact3.relationship", "Friend")}\"")
        buildConfigField("String", "CONTACT3_PHONE", "\"${local("guard.contact3.phone", "+5500000000003")}\"")
    }

    // Liga a geração da classe BuildConfig (desligada por padrão nas versões novas do Android Gradle Plugin).
    buildFeatures {
        buildConfig = true
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
