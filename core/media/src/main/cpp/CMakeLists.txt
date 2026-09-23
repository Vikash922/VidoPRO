cmake_minimum_required(VERSION 3.22.1)
project("vidopro_native")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_definitions(-O3 -ffast-math)

add_library(
    vidopro_native
    SHARED
    vidopro_native.cpp
    AdjustmentProcessor.cpp
    BlendEngine.cpp
    AudioBeatDetector.cpp
    KeyframeEngine.cpp
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(
    vidopro_native
    ${log-lib}
    ${jnigraphics-lib}
    m
)
