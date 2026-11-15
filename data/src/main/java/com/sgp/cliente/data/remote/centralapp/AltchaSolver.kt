package com.sgp.cliente.data.remote.centralapp

import android.util.Base64
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Solves the ALTCHA proof-of-work required by `centralapp/login` when the
 * instance enables `habilitar_captcha_login`: find `n` in `[0, maxNumber]`
 * such that `SHA-256(salt + n) == challenge`, then return the base64 JSON
 * payload the API expects.
 *
 * The challenge hex is decoded once and digests are compared as raw bytes —
 * formatting hex inside the loop would allocate ~64 chars per candidate and
 * turn a sub-second solve into a minute of GC pressure on a phone.
 */
class AltchaSolver @Inject constructor() {

    suspend fun solve(challenge: CaptchaChallengeDto): String? =
        withContext(Dispatchers.Default) {
            val digest = MessageDigest.getInstance("SHA-256")
            val target = challenge.challenge.hexToBytes() ?: return@withContext null
            val started = System.nanoTime()
            val saltBytes = challenge.salt.toByteArray()

            for (n in 0..challenge.maxNumber) {
                if (n and 0xFFFF == 0) ensureActive()
                digest.update(saltBytes)
                val hash = digest.digest(n.toString().toByteArray())
                if (MessageDigest.isEqual(hash, target)) {
                    return@withContext buildPayload(
                        challenge = challenge,
                        number = n,
                        tookMillis = (System.nanoTime() - started) / 1_000_000,
                    )
                }
            }
            null
        }

    private fun buildPayload(
        challenge: CaptchaChallengeDto,
        number: Int,
        tookMillis: Long,
    ): String {
        val payload = buildJsonObject {
            put("algorithm", challenge.algorithm)
            put("challenge", challenge.challenge)
            put("number", number)
            put("salt", challenge.salt)
            put("signature", challenge.signature)
            put("took", tookMillis)
        }
        return Base64.encodeToString(
            payload.toString().toByteArray(),
            Base64.NO_WRAP,
        )
    }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { i ->
                ((Character.digit(this[2 * i], 16) shl 4) +
                    Character.digit(this[2 * i + 1], 16)).toByte()
            }
        }.getOrNull()
    }
}
