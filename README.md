# مشروع تطبيق "الحافظة الذكية" (Smart Clipboard Manager)

يوضح هذا المستند المكونات البرمجية وملفات التصميم الخاصة بتطبيق **الحافظة الذكية** لأجهزة أندرويد بلغة Kotlin وتصميم Material Design.

---

## 📦 ملفات المشروع المتاحة في مساحة العمل (Workspace)

1. **`AppDatabase.kt`**: يحتوي على كيان قاعدة البيانات (Room Entity: `ClipboardItem`)، واجهة العمليات (DAO)، وإنشاء قاعدة البيانات محليًا.
2. **`ClipboardRepository.kt`**: طبقة البيانات (Repository) المسؤولة عن إدارة العمليات والتحقق من عدم تكرار النصوص (Deduplication) وتحديد الحد الأقصى للسجل (100 عنصر).
3. **`ClipboardViewModel.kt`**: منطق العرض (ViewModel) المتصل بقاعدة البيانات ودعم البحث التلقائي باستخدام StateFlow و LiveData.
4. **`ClipboardService.kt`**: خدمة الخلفية (Foreground Service) التي تراقب `ClipboardManager` باستمرار وتحفظ النصوص المنسوخة تلقائيًا مع إشعار دائم.
5. **`ClipboardAdapter.kt`**: محول القائمة (`RecyclerView Adapter`) لعرض النصوص وتاريخ النسخ، وأزرار النسخ والتثبيت والحذف.
6. **`MainActivity.kt`**: الشاشة الرئيسية للتطبيق، تعرض السجل، البحث، وإدارة الأزرار العائمة والإشعارات.
7. **`activity_main.xml`**: تصميم واجهة الشاشة الرئيسية (Toolbar, RecyclerView, Empty View, FAB).
8. **`item_clipboard.xml`**: تصميم عنصر السجل داخل القائمة (CardView مع أزرار التحكم).
9. **`main_menu.xml`**: شريط البحث والإعدادات العلوي.
10. **`AndroidManifest.xml`**: إعدادات التطبيق والأذونات المطلوبة (`FOREGROUND_SERVICE`).

---

## ⚙️ إعدادات `build.gradle` (Dependencies المطلوبة)

لإعداد المشروع في Android Studio، أضف الاعتبارات التالية في ملف `build.gradle.kts` (Module: app):

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // أو كاتب ksp حسب تفضيلك لـ Room
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smart.clipboard"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Lifecycle & ViewModel
    val lifecycleVersion = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
}
```

---

## 🚀 المميزات المطبقة:
- ✅ **مراقبة الحافظة التلقائية**: عبر Foreground Service وتحديثات `ClipboardManager`.
- ✅ **منع التكرار المتتالي**: تجاهل النسخ المتكرر لنفس النص المتتالي لعدم إشغال الذاكرة.
- ✅ **تثبيت العناصر (Pinning)**: إمكانية تثبيت النصوص المهمة في أعلى القائمة وعدم حذفها تلقائيًا عند تنظيف السجل.
- ✅ **البحث الذكي**: تصفية النصوص المنسوخة فوراً عبر شريط البحث العلوي.
- ✅ **تصميم أنيق متوافق مع Material Design**: مع دعم الألوان المحددة (أخضر `#4CAF50` وبرتقالي `#FF9800`).
