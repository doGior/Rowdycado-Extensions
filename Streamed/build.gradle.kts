@file:Suppress("UnstableApiUsage")

// use an integer for version numbers
version = 1


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Live stream of different sports"
    authors = listOf("doGior")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1


    requiresResources = true
    language = "en"

    // random cc logo i found
    iconUrl = "https://streamed.su/favicon.png"
}

android {
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        minSdk = 26
        compileSdk =33
        targetSdk = 33
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
}
