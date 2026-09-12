package com.liskovsoft.smartyoutubetv2.common.vot;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AnonymousVotSessionProviderTest {

    private static class MockVotHttp extends VotHttp {
        private final AtomicInteger mCallCount = new AtomicInteger(0);
        private volatile byte[] mMockResponse;
        private volatile boolean mThrowIo = false;

        public void setMockResponse(byte[] response) {
            mMockResponse = response;
        }

        public void setThrowIo(boolean throwIo) {
            mThrowIo = throwIo;
        }

        public int getCallCount() {
            return mCallCount.get();
        }

        @Override
        public byte[] postProtobuf(String path, byte[] body, Map<String, String> headers) throws IOException {
            mCallCount.incrementAndGet();
            if (mThrowIo) {
                throw new IOException("Simulated network error");
            }
            if (mMockResponse != null) {
                return mMockResponse;
            }
            VotWireWriter w = new VotWireWriter();
            w.writeString(1, "mock-secret-key-123456789");
            w.writeInt32(2, 3600);
            return w.toByteArray();
        }
    }

    private MockVotHttp mMockHttp;
    private AnonymousVotSessionProvider mProvider;

    @Before
    public void setUp() {
        mMockHttp = new MockVotHttp();
        mProvider = new AnonymousVotSessionProvider(mMockHttp);
    }

    @Test
    public void testInitialStateHasNoValidSession() {
        assertFalse("Initial provider must not have valid session", mProvider.hasValidSession());
    }

    @Test
    public void testGetOrCreateSessionCreatesValidSession() throws IOException {
        VotSession session = mProvider.getOrCreateSession();
        assertNotNull("Session must not be null", session);
        assertEquals("mock-secret-key-123456789", session.secretKey);
        assertEquals(3600, session.expiresSec);
        assertNotNull("UUID must be generated", session.uuid);
        assertEquals(32, session.uuid.length());
        assertTrue("Session must be valid", mProvider.hasValidSession());
        assertEquals(1, mMockHttp.getCallCount());
    }

    @Test
    public void testSessionReuseDoesNotDuplicateNetworkRequests() throws IOException {
        VotSession s1 = mProvider.getOrCreateSession();
        VotSession s2 = mProvider.getOrCreateSession();

        assertSame("Must return same cached session instance", s1, s2);
        assertEquals("Network must only be called once for valid session", 1, mMockHttp.getCallCount());
    }

    @Test
    public void testRefreshSessionForcesNewNetworkRequest() throws IOException {
        VotSession s1 = mProvider.getOrCreateSession();
        VotSession s2 = mProvider.refreshSession();

        assertNotNull(s2);
        assertEquals("Network must be called twice", 2, mMockHttp.getCallCount());
    }

    @Test
    public void testInvalidateSessionClearsSession() throws IOException {
        mProvider.getOrCreateSession();
        assertTrue(mProvider.hasValidSession());

        mProvider.invalidateSession();
        assertFalse(mProvider.hasValidSession());
    }

    @Test
    public void testNetworkFailureThrowsException() {
        mMockHttp.setThrowIo(true);
        try {
            mProvider.getOrCreateSession();
            fail("Expected IOException on network error");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Simulated network error"));
        }
        assertFalse(mProvider.hasValidSession());
    }

    @Test
    public void testConcurrentRequestsPreventStampede() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<VotSession> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    VotSession s = mProvider.getOrCreateSession();
                    results.add(s);
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount, results.size());
        assertEquals("Network should only be called once despite 10 concurrent callers", 1, mMockHttp.getCallCount());
        VotSession first = results.get(0);
        for (VotSession s : results) {
            assertSame("All threads must receive identical session", first, s);
        }
    }

    @Test
    public void testHeadersUseSessionWhenValid() throws IOException {
        byte[] testBody = "test-protobuf-payload".getBytes(StandardCharsets.UTF_8);
        String path = "/video-translation/translate";

        Map<String, String> h1 = mProvider.getTranslateHeaders(testBody, path, false);
        assertNotNull(h1.get("Vtrans-Signature"));
        assertNotNull(h1.get("Sec-Vtrans-Token"));
        assertNull(h1.get("Sec-Vtrans-Sk"));
        assertNull(h1.get("Authorization"));

        mProvider.getOrCreateSession();
        Map<String, String> h2 = mProvider.getTranslateHeaders(testBody, path, true);
        assertNotNull(h2.get("Vtrans-Signature"));
        assertNotNull(h2.get("Sec-Vtrans-Sk"));
        assertNotNull(h2.get("Sec-Vtrans-Token"));
        assertEquals("mock-secret-key-123456789", h2.get("Sec-Vtrans-Sk"));
        assertNull("Anonymous provider must never add Authorization header", h2.get("Authorization"));
    }
}