package com.cortex.app.domain.graph

/**
 * Seed term lists for dictionary-based extraction. Grounded in GraphEngine.md's own
 * named topic clusters (robotics, research, writing, debugging, planning, study) rather
 * than any one person's stack — robotics/ML terms sit alongside general web/mobile ones
 * on purpose, since Cortex itself is a general-purpose Android app.
 *
 * This is intentionally a *seed* list. Growing it doesn't require touching
 * [EntityExtractor] — just add terms here. Multi-word terms are supported (see
 * [WordTrie]).
 */
object EntityDictionaries {

    val programmingLanguages = listOf(
        "kotlin", "java", "python", "c++", "c", "c#", "rust", "go", "golang",
        "javascript", "typescript", "swift", "dart", "scala", "ruby", "matlab",
        "r", "julia", "bash", "shell", "sql", "haskell", "lua", "objective-c"
    )

    val frameworks = listOf(
        "ros", "ros2", "ros 2", "gazebo", "pytorch", "tensorflow", "keras",
        "jetpack compose", "android sdk", "react", "react native", "spring", "spring boot",
        "django", "flask", "fastapi", "opencv", "moveit", "moveit2", "qt", "unity",
        "unreal engine", "node.js", "express", "angular", "vue", "flutter", "jetpack"
    )

    val libraries = listOf(
        "kotlinx.coroutines", "kotlinx.serialization", "room", "retrofit", "okhttp",
        "numpy", "scipy", "pandas", "eigen", "boost", "junit", "mockito", "point cloud library",
        "pcl", "glm", "protobuf", "grpc-java", "dagger", "hilt", "coil", "glide", "rxjava"
    )

    val hardware = listOf(
        "arduino", "raspberry pi", "jetson nano", "jetson orin", "jetson xavier",
        "stm32", "esp32", "esp8266", "lidar", "imu", "teensy", "pixhawk", "servo motor",
        "stepper motor", "encoder", "fpga", "beaglebone", "rover", "gimbal", "actuator",
        "microcontroller", "gpu", "tpu", "raspberry pi pico"
    )

    val technology = listOf(
        "docker", "kubernetes", "mqtt", "bluetooth", "wifi", "can bus", "slam",
        "pid controller", "kalman filter", "grpc", "rest api", "graphql", "websocket",
        "firmware", "rtos", "real-time operating system", "ci/cd", "github actions",
        "opengl", "vulkan", "cuda", "onnx", "tensorrt"
    )

    val osNames = listOf(
        "android", "ios", "windows", "macos", "linux", "ubuntu", "debian", "fedora",
        "arch linux", "chromeos", "watchos", "wearos", "freertos", "zephyr", "yocto",
        "raspbian", "raspberry pi os"
    )

    fun buildTechDictionaryTrie(): WordTrie<EntityKind> {
        val trie = WordTrie<EntityKind>()
        programmingLanguages.forEach { trie.insert(it, EntityKind.PROGRAMMING_LANGUAGE) }
        frameworks.forEach { trie.insert(it, EntityKind.FRAMEWORK) }
        libraries.forEach { trie.insert(it, EntityKind.LIBRARY) }
        hardware.forEach { trie.insert(it, EntityKind.HARDWARE) }
        technology.forEach { trie.insert(it, EntityKind.TECHNOLOGY) }
        osNames.forEach { trie.insert(it, EntityKind.OS_NAME) }
        return trie
    }
}
