import com.virtuslab.gitmachete.buildsrc.*
// import net.ltgt.gradle.errorprone.errorprone

plugins {
//  id("net.ltgt.errorprone") version "3.1.0"
}

dependencies {
  implementation(project(":backend:api"))
  implementation(project(":branchLayout:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:file"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))

//  // Annotator stuff
//  compileOnly("com.google.code.findbugs:jsr305:3.0.2")
//  errorprone("com.google.errorprone:error_prone_core:2.22.0")

//  // Annotator Scanner Checker
//  annotationProcessor("edu.ucr.cs.riple.annotator:annotator-scanner:1.3.8-SNAPSHOT")
}

addIntellijToCompileClasspath(withGit4Idea = true)
apacheCommonsText()
applyKotlinConfig()
lombok()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()

val scannerConfig = "${project.rootDir}/annotator-out/scanner.xml"
tasks.named("compileJava", JavaCompile::class) {
  // The check defaults to a warning, bump it up to an error for the main sources
//  options.errorprone.error("AnnotatorScanner")
//  options.errorprone.option("AnnotatorScanner:ConfigPath", scannerConfig)
}
