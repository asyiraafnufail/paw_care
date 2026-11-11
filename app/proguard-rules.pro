# PawCare ProGuard Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Firebase data models for Firestore reflection deserialization
-keep class com.pawcare.app.data.model.** { *; }
-keepclassmembers class com.pawcare.app.data.model.** { *; }

# Keep ViewModel classes
-keep class com.pawcare.app.viewmodel.** extends androidx.lifecycle.ViewModel { *; }
