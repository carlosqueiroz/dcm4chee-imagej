cmake_minimum_required (VERSION 2.8)

project(Dcm4chee_Import)

find_package(Java REQUIRED)
include(UseJava)

set(CMAKE_JAVA_COMPILE_FLAGS "-source" "1.6" "-target" "1.6")
set(CMAKE_JAVA_INCLUDE_PATH /usr/share/java/ij.jar)

# The name of an ImageJ plugin must contain an underscore
# http://imagejdocu.tudor.lu/doku.php?id=howto:plugins:howto_create_an_imagej_plugin_jar_file
add_jar(Dcm4chee_Import
  # Sources of the plugin
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/DicomDecoder.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/Dcm4chee.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/Dcm4cheeConfigurationDialog.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/Dcm4cheeConnection.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/PreviewPanel.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/SelectImageDialog.java
  ${CMAKE_SOURCE_DIR}/com/Dcm4chee/HttpsTrustModifier.java

  # Sources of the "json-simple" toolkit
  ${CMAKE_SOURCE_DIR}/org/json/simple/ItemList.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/JSONArray.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/JSONAware.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/JSONObject.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/JSONStreamAware.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/JSONValue.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/ContainerFactory.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/ContentHandler.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/JSONParser.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/ParseException.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/Yylex.java
  ${CMAKE_SOURCE_DIR}/org/json/simple/parser/Yytoken.java
  )

# Add the "plugins.config" file that is expected by ImageJ in the JAR file
add_custom_command(TARGET Dcm4chee_Import
  POST_BUILD
  COMMAND jar uf ${CMAKE_CURRENT_BINARY_DIR}/Dcm4chee_Import.jar plugins.config
  WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
  COMMENT "Packaging the configuration file" VERBATIM
  )
