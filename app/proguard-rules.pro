# SGP Cliente — R8 rules.
# Retrofit service interface is consumed reflectively via dynamic proxy.
-keep interface com.sgp.cliente.data.remote.centralapp.CentralAppApiService { *; }

# Kotlinx-serialization DTOs: @SerialName fields drive the wire format.
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}
