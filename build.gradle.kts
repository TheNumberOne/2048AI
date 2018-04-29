plugins {
    `build-scan`
    kotlin("jvm") version "1.2.40"
}

group = "io.github.thenumberone"

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib", "1.2.40"))
    testImplementation("junit:junit:4.12")
}

//println(java.sourceSets["main"]["kotlin"])

buildScan {
    setLicenseAgreementUrl("https://gradle.com/terms-of-service") 
    setLicenseAgree("yes")

    publishAlways() 
}