cmake_minimum_required(VERSION 3.22.1)
project("queglass" CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Reusable QUEUEGLASS core library (shared with the Linux backend).
set(QUEUEGLASS_CORE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../core)

add_library(queglass_core STATIC
        ${QUEUEGLASS_CORE_DIR}/src/OrderBook.cpp
        ${QUEUEGLASS_CORE_DIR}/src/Candle.cpp
        ${QUEUEGLASS_CORE_DIR}/src/Engine.cpp
        ${QUEUEGLASS_CORE_DIR}/src/EventGenerator.cpp
        ${QUEUEGLASS_CORE_DIR}/src/Volatility.cpp
        ${QUEUEGLASS_CORE_DIR}/src/Trace.cpp
        ${QUEUEGLASS_CORE_DIR}/src/ExecutionEngine.cpp
        ${QUEUEGLASS_CORE_DIR}/src/StrategyModule.cpp
        ${QUEUEGLASS_CORE_DIR}/src/IntegrationHub.cpp
        ${QUEUEGLASS_CORE_DIR}/src/McpServer.cpp
        ${QUEUEGLASS_CORE_DIR}/src/RestApi.cpp
        ${QUEUEGLASS_CORE_DIR}/src/InternalPlugin.cpp)

target_include_directories(queglass_core PUBLIC ${QUEUEGLASS_CORE_DIR}/include)
target_compile_options(queglass_core PRIVATE -O2)

add_library(${CMAKE_PROJECT_NAME} SHARED
        queglass.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        queglass_core
        android
        log)
