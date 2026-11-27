package com.hoangpha.cityguard.data.local

// TV2 - Trần Hữu Thịnh: Kiểm thử phần TV2 và các điểm tích hợp; fake chỉ dùng trong test, không thay nguồn dữ liệu runtime của app.

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SessionManagerTest {
    private lateinit var manager: SessionManager
    @Before fun setup() {
        manager = SessionManager(ApplicationProvider.getApplicationContext<Context>())
        runBlocking { manager.clearSession() }
    }
    @After fun tearDown() { runBlocking { manager.clearSession() } }
    @Test fun sessionSurvivesNewManagerInstance() = runTest {
        manager.saveSession("access", "7", "refresh", 30_000)
        val reopened = SessionManager(ApplicationProvider.getApplicationContext<Context>())
        assertEquals("access", reopened.getToken())
        assertEquals("7", reopened.getUserId())
        assertEquals("refresh", reopened.session.first()?.refreshToken)
    }
    @Test fun old401CannotClearNewSession() = runTest {
        manager.saveSession("new-access", "8", "new-refresh", 30_000)
        manager.clearIfTokenMatches("old-access")
        assertEquals("new-access", manager.getToken())
    }
    @Test fun logoutRemovesAllStoredSessionValues() = runTest {
        manager.saveSession("access", "7", "refresh", 30_000)
        manager.clearSession()
        assertNull(manager.session.first())
        assertNull(manager.getToken())
        assertNull(manager.getUserId())
    }
}
