plugins {
	java
	jacoco
	checkstyle
	id("org.springframework.boot") version "3.5.10"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.4"
	id("org.sonarqube") version "7.2.2.6593"
}

group = "com.mysawit"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

val grpcSpringStarterVersion = "3.1.0.RELEASE"
val grpcVersion = "1.63.0"
val protobufVersion = "3.25.3"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("net.devh:grpc-spring-boot-starter:$grpcSpringStarterVersion")
	implementation("io.grpc:grpc-protobuf:$grpcVersion")
	implementation("io.grpc:grpc-stub:$grpcVersion")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	compileOnly("jakarta.annotation:jakarta.annotation-api:1.3.5")
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.grpc:grpc-testing:$grpcVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testRuntimeOnly("com.h2database:h2")
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:$protobufVersion"
	}
	plugins {
		create("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
		}
	}
	generateProtoTasks {
		all().forEach {
			it.plugins {
				create("grpc")
			}
		}
	}
}

sourceSets {
	main {
		java {
			srcDirs(
				"build/generated/source/proto/main/java",
				"build/generated/source/proto/main/grpc"
			)
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Test>("unitTest") {
	description = "Runs the unit tests."
	group = "verification"

	filter {
		excludeTestsMatching("*FunctionalTest")
	}
}

tasks.register<Test>("functionalTest") {
	description = "Runs the functional tests."
	group = "verification"

	filter {
		includeTestsMatching("*FunctionalTest")
	}
}

tasks.test {
	filter {
		excludeTestsMatching("*FunctionalTest")
	}

	finalizedBy(tasks.jacocoTestReport)
}

val functionalTestTask = tasks.named<Test>("functionalTest")

tasks.jacocoTestReport {
	dependsOn(tasks.test, functionalTestTask)
	executionData.setFrom(
		fileTree(layout.buildDirectory) {
			include("jacoco/*.exec")
		}
	)
	reports {
		xml.required.set(true)
	}
	classDirectories.setFrom(
		files(classDirectories.files.map {
			fileTree(it) {
				exclude(
					"**/proto/**",
					"**/grpc/**Grpc.class",
					"**/grpc/**Grpc$*.class",
					"**/integration/client/**",
					"**/PengirimanApplication.class",
					"**/config/**"
				)
			}
		})
	)
}

checkstyle {
	toolVersion = "10.17.0"
	configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.withType<Checkstyle>().configureEach {
	source("src/main/java", "src/test/java")
	include("**/*.java")
	exclude("**/proto/**")
}

sonar {
	properties {
		property("sonar.projectKey", "KEL-ADPRO_MySawit-backend-manage-pengiriman")

		property("sonar.organization", "kel-adpro")

		property("sonar.host.url", "https://sonarcloud.io")

		property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
	}
}
