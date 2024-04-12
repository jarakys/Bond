cmake_minimum_required(VERSION 3.4.1)
#[[set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} --coverage")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} --coverage")]]

# BASE PATH  = YOUR PROJECT ROOT DIRECTORY
#/home/leometric/Desktop/test/ndk-samples-master/hello-jni/app/src/main/cpp
#set(BASE_PATH "C:/Android_new/masmak-android")
#set(BASE_PATH "D:/FreeLancer/masmak-android")
#set(BASE_PATH "/Users/Valerio/Project/masmak/android/masmak-android")
set(BASE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../../../../")


set(distribution_DIR ${BASE_PATH}/distribution)
add_library(hello-jni SHARED
        blackboxcalls.c blackbox.c)

find_library (log-lib log)
#add the open ssl crypto lib
add_library(libcrypto STATIC IMPORTED)
set_target_properties(libcrypto PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/arm64-v8a/lib/libcrypto.a)

# add the open ssl ssl lib

add_library(libssl STATIC IMPORTED)

set_target_properties(libssl PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/openssl/arm64-v8a/lib/libssl.a)


add_library(libopus STATIC IMPORTED)

set_target_properties(libopus PROPERTIES IMPORTED_LOCATION  ${distribution_DIR}/arm64-v8a/libopustool.a)


add_library(mylibjpeg STATIC IMPORTED)

set_target_properties(mylibjpeg
        PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/arm64-v8a/mylibjpeg.a)

#[[add_library(libblackbox STATIC IMPORTED)
set_target_properties(libblackbox PROPERTIES IMPORTED_LOCATION
        /home/leometric/Desktop/test/ndk-samples-master/hello-jni/distribution/arm64-v8a/libblackbox.a)]]

#add to target_include_directories

target_include_directories(hello-jni PRIVATE ${distribution_DIR}/include
        ${BASE_PATH}/app/src/main/cpp/include)


# Include libraries needed for hello-jni lib
target_link_libraries(hello-jni
#        libblackbox
        libssl
        libcrypto
        libopus
        mylibjpeg
        ${log-lib})
