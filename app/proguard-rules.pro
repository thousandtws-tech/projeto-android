# SGP Cliente — R8 rules.
# Retrofit service interface is consumed reflectively via dynamic proxy.
-keep interface com.sgp.cliente.data.remote.centralapp.CentralAppApiService { *; }

# Kotlinx-serialization DTOs: @SerialName fields drive the wire format.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# Timber: release trees already drop < WARN — remove those call sites
# entirely so verbose/debug logging costs nothing in release builds.
-assumenosideeffects class timber.log.Timber* {
    public *** tag(...);
    public *** v(...);
    public *** d(...);
    public *** i(...);
}
