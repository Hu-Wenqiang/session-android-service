package org.whispersystems.signalservice.loki

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.whispersystems.signalservice.internal.util.Base64

internal data class LokiMessage(
    /**
     * The hex encoded public key of the receiver.
     */
    internal val destination: String,
    /**
     * The content of the message.
     */
    internal val data: String,
    /**
     * The time to live for the message in milliseconds.
     */
    internal val ttl: Int,
    /**
     * Whether this message is a ping.
     */
    internal val isPing: Boolean,
    /**
     * When the proof of work was calculated, if applicable (P2P messages don't require proof of work).
     *
     * - Note: Expressed as milliseconds since 00:00:00 UTC on 1 January 1970.
     */
    internal var timestamp: Long? = null,
    /**
     * The base 64 encoded proof of work, if applicable (P2P messages don't require proof of work).
     */
    internal var nonce: String? = null
) {

    internal companion object {

        internal fun from(signalMessage: Map<*, *>): LokiMessage? {
            val wrappedMessage = ByteArray(0)
            val data = Base64.encodeBytes(wrappedMessage)
            val destination = signalMessage["destination"] as String
            var ttl = LokiAPI.defaultMessageTTL
            val messageTTL = signalMessage["ttl"] as Int?
            if (messageTTL != null && messageTTL != 0) { ttl = messageTTL }
            val isPing = signalMessage["isPing"] as Boolean
            return LokiMessage(destination, data, ttl, isPing)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    internal fun calculatePoW(): Promise<LokiMessage, Exception> {
        return task {
            val now = System.currentTimeMillis()
            val nonce = ProofOfWork.calculate(data, destination, now, ttl)
            if (nonce != null ) {
                copy(nonce = nonce, timestamp = now)
            } else {
                throw LokiAPI.Error.ProofOfWorkCalculationFailed
            }
        }
    }

    internal fun toJSON(): Map<String, String> {
        val result = mutableMapOf( "pubKey" to destination, "data" to data, "ttl" to ttl.toString() )
        val timestamp = timestamp
        val nonce = nonce
        if (timestamp != null && nonce != null) {
            result["timestamp"] = timestamp.toString()
            result["nonce"] = nonce
        }
        return result
    }
}