@echo off
REM ========= CONFIG ==========
SET FRAMEWORK_NAME=Framework_ETU003222
SET LIB_DIR=lib
SET JAR_NAME=%FRAMEWORK_NAME%.jar
SET APP_LIB_DIR=..\TestFramework_ETU003222\lib
SET SERVLET_API_JAR=%LIB_DIR%\servlet-api.jar
REM ===========================

echo Compilation directe des sources du framework ...
javac -cp "%SERVLET_API_JAR%" -d . src\com\itu\ETU003222\*.java

REM Verifie que FrontServlet.class a ete genere
if not exist src\com\itu\ETU003222\FrontServlet.class (
    echo ERREUR : FrontServlet.class n'a pas ete compile !
    pause
    exit /b 1
)

echo Creation du JAR du framework ...
jar -cvf %JAR_NAME% -C . src\com\itu\ETU003222

echo Verification du contenu du JAR :
jar -tf %JAR_NAME% | find "FrontServlet.class"

echo Copie du JAR dans TestFramework_ETU003222/lib ...
if not exist %APP_LIB_DIR% mkdir %APP_LIB_DIR%
copy %JAR_NAME% %APP_LIB_DIR%

echo ============================
echo Framework compile et JAR genere : %JAR_NAME%
echo Copie dans TestFramework_ETU003222\lib\
echo ============================