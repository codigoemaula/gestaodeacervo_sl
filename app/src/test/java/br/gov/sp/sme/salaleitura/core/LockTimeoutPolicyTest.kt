package br.gov.sp.sme.salaleitura.core

import br.gov.sp.sme.salaleitura.feature.security.LockTimeoutPolicy
import org.junit.Assert.*
import org.junit.Test

class LockTimeoutPolicyTest {
    @Test fun `first use always requires local device authentication`() {
        assertTrue(LockTimeoutPolicy.requiresUnlock(false, null, 100_000L))
    }

    @Test fun `ordinary app navigation and short background do not require another unlock`() {
        assertFalse(LockTimeoutPolicy.requiresUnlock(true, null, 100_000L))
        assertFalse(LockTimeoutPolicy.requiresUnlock(true, 100_000L, 399_999L))
    }

    @Test fun `five minutes away triggers unlock again`() {
        assertTrue(LockTimeoutPolicy.requiresUnlock(true, 100_000L, 400_000L))
    }

    @Test fun `clock rollback errs on requiring authentication`() {
        assertTrue(LockTimeoutPolicy.requiresUnlock(true, 400_000L, 100_000L))
    }
}
