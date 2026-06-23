# Room and Kotlin metadata required for generated database implementations.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
