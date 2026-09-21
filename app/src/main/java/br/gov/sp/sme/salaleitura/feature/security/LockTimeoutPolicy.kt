package br.gov.sp.sme.salaleitura.feature.security

/** Monotonic elapsedRealtime, not wall-clock time. Navigation never changes lock state. */
object LockTimeoutPolicy {
    private const val TIMEOUT_MS = 5L * 60L * 1000L

    fun requiresUnlock(unlocked: Boolean, backgroundAt: Long?, now: Long): Boolean {
        if (!unlocked) return true
        if (backgroundAt == null) return false
        val elapsed = now - backgroundAt
        return elapsed < 0 || elapsed >= TIMEOUT_MS
    }
}
