#!/bin/bash

echo "=========================================="
echo "  Smart Clipboard - Project Restructure"
echo "=========================================="
echo ""

# 1. إنشاء المجلدات
echo "[1/8] Creating directory structure..."
mkdir -p app/src/main/java/com/smart/clipboard/adapter
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/menu
mkdir -p app/src/main/res/values
mkdir -p gradle/wrapper
echo "✓ Directories created"

# 2. نقل ملفات Kotlin
echo "[2/8] Moving Kotlin files..."
mv MainActivity.kt app/src/main/java/com/smart/clipboard/ 2>/dev/null || echo "  Note: MainActivity.kt not found in root"
mv AppDatabase.kt app/src/main/java/com/smart/clipboard/ 2>/dev/null || echo "  Note: AppDatabase.kt not found in root"
mv ClipboardRepository.kt app/src/main/java/com/smart/clipboard/ 2>/dev/null || echo "  Note: ClipboardRepository.kt not found in root"
mv ClipboardService.kt app/src/main/java/com/smart/clipboard/ 2>/dev/null || echo "  Note: ClipboardService.kt not found in root"
mv ClipboardViewModel.kt app/src/main/java/com/smart/clipboard/ 2>/dev/null || echo "  Note: ClipboardViewModel.kt not found in root"
mv ClipboardAdapter.kt app/src/main/java/com/smart/clipboard/adapter/ 2>/dev/null || echo "  Note: ClipboardAdapter.kt not found in root"
echo "✓ Kotlin files moved"

# 3. نقل ملفات XML
echo "[3/8] Moving XML files..."
mv AndroidManifest.xml app/src/main/ 2>/dev/null || echo "  Note: AndroidManifest.xml not found in root"
mv activity_main.xml app/src/main/res/layout/ 2>/dev/null || echo "  Note: activity_main.xml not found in root"
mv item_clipboard.xml app/src/main/res/layout/ 2>/dev/null || echo "  Note: item_clipboard.xml not found in root"
mv main_menu.xml app/src/main/res/menu/ 2>/dev/null || echo "  Note: main_menu.xml not found in root"
echo "✓ XML files moved"

# 4. إنشاء ملفات values
echo "[4/8] Creating values files..."

cat > app/src/main/res/values/strings.xml << 'EOF'
<resources>
    <string name="app_name">Smart Clipboard</string>
</resources>
EOF

cat > app/src/main/res/values/colors.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
EOF

cat > app/src/main/res/values/themes.xml << 'EOF'
<resources>
    <style name="Theme.SmartClipboard" parent="Theme.Material3.DayNight.NoActionBar">
    </style>
</resources>
EOF

echo "✓ Values files created"

# 5. إنشاء ملفات Gradle
echo "[5/8] Creating Gradle files..."

cat > settings.gradle << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Smart Clipboard"
include ':app'
EOF

cat > build.gradle << 'EOF'
plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
    id 'com.google.devtools.ksp' version '1.9.0-1.0.13' apply false
}
EOF

cat > app/build.gradle << 'EOF'
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.devtools.ksp'
}

android {
    namespace 'com.smart.clipboard'
    compileSdk 34

    defaultConfig {
        applicationId "com.smart.clipboard"
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    ksp 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
}
EOF

cat > app/proguard-rules.pro << 'EOF'
# ProGuard rules
EOF

echo "✓ Gradle files created"

# 6. إنشاء Gradle Wrapper
echo "[6/8] Creating Gradle wrapper..."

cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# إنشاء gradlew
cat > gradlew << 'GRADLEW_EOF'
#!/bin/sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
app_path=$0

while
    APP_HOME=${app_path%"${app_path##*/}"}
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in
      /*)   app_path=$link ;;
      *)    app_path=$APP_HOME$link ;;
    esac
done

APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in
  CYGWIN* )         cygwin=true  ;;
  Darwin* )         darwin=true  ;;
  MSYS* | MINGW* )  msys=true    ;;
  NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set"
fi

if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in
      max*)
        MAX_FD=$( ulimit -H -n ) || warn "Could not query maximum file descriptor limit"
    esac
    case $MAX_FD in
      '' | soft) :;;
      *)
        ulimit -n "$MAX_FD" || warn "Could not set maximum file descriptor limit"
    esac
fi

set -- \
        "-Dorg.gradle.appname=$APP_BASE_NAME" \
        -classpath "$CLASSPATH" \
        org.gradle.wrapper.GradleWrapperMain \
        "$@"

if ! command -v xargs >/dev/null 2>&1
then
    die "xargs is not available"
fi

eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs -n1 |
        sed ' s~[^-:a-zA-Z0-9_./\\@]~\\&~g; ' |
        tr '\n' ' '
    )" '"$@"'

exec "$JAVACMD" "$@"
GRADLEW_EOF

chmod +x gradlew
echo "✓ Gradle wrapper created"

# 7. تحديث workflow
echo "[7/8] Updating GitHub Actions workflow..."

mkdir -p .github/workflows

cat > .github/workflows/build-apk.yml << 'EOF'
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle
      
      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew
      
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: Smart-Clipboard-APK
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

echo "✓ Workflow updated"

# 8. رفع التغييرات
echo "[8/8] Pushing changes to GitHub..."
git add .
git commit -m "Restructure Android project with correct layout and add Gradle wrapper"
git push origin main

echo ""
echo "=========================================="
echo "  ✓ DONE! Project restructured successfully"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Go to https://github.com/Nadr59/Smart-Clipboard-/actions"
echo "2. Wait for the build to complete (5-10 minutes)"
echo "3. Download the APK from Artifacts"
echo ""
