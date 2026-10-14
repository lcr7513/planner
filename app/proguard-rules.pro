# ==============================================================================
# 학습매니저 (EduManage) - 프로덕션 릴리스 ProGuard / R8 최적화 규칙
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 기본 코드 축소 및 최적화 설정
# ------------------------------------------------------------------------------
-repackageclasses ''
-allowaccessmodification
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**

# 스택 트레이스 디버깅 정보 보존 (크래시 분석용)
-keepattributes SourceFile,LineNumberTable,EnclosingMethod,InnerClasses,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 2. AndroidX & Jetpack Compose 관련 규칙
# ------------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.ui.platform.AndroidComposeView { *; }

# ViewModel & Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ------------------------------------------------------------------------------
# 3. AndroidX Room Database 엔터티 및 DAO 규칙
# ------------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract *;
}
# Room RoomDatabase.Builder 마이그레이션 및 자동 생성 클래스 보존
-keep class com.example.data.db.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.dao.** { *; }

# ------------------------------------------------------------------------------
# 4. Moshi JSON Serialization 규칙
# ------------------------------------------------------------------------------
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonQualifier *;
}
-keep @com.squareup.moshi.JsonClass(generateAdapter = "true") class * { *; }
-keep class *JsonAdapter {
    <init>(...);
}
-keepnames class com.example.data.api.** { *; }
-keepclassmembers class com.example.data.api.** { *; }

# ------------------------------------------------------------------------------
# 5. Retrofit & OkHttp 네트워크 라이브러리 규칙
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ------------------------------------------------------------------------------
# 6. Firebase & Google Play Services 관련 규칙
# ------------------------------------------------------------------------------
-keepattributes *JavascriptInterface*
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

# ------------------------------------------------------------------------------
# 7. Kotlin Coroutines & Flow 관련 규칙
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 8. 앱 모델 및 UI 상태 클래스 (난독화 제외로 데이터 안전 보장)
# ------------------------------------------------------------------------------
-keep class com.example.ui.viewmodel.LearningUiState { *; }
-keep class com.example.ui.viewmodel.StudentProfile { *; }
-keep class com.example.ui.screens.QuizLaunchConfig { *; }
-keep class com.example.data.service.AiLearningAnalysisResult { *; }
-keep class com.example.data.service.GeminiStudyAnalyzer { *; }

