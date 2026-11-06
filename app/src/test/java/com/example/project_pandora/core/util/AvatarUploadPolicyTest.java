package com.example.project_pandora.core.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class AvatarUploadPolicyTest {
    @Test public void acceptsFiveMegabyteBoundaryAndRejectsEmptyOrOversizedUploads() {
        assertTrue(AvatarUploadPolicy.accepts(3L * 1024 * 1024));
        assertTrue(AvatarUploadPolicy.accepts(5L * 1024 * 1024));
        assertFalse(AvatarUploadPolicy.accepts(5L * 1024 * 1024 + 1));
        assertFalse(AvatarUploadPolicy.accepts(0));
    }
    @Test public void cameraSizedImagesAreSampledWithoutEnlargingSmallPictures() {
        assertEquals(1, AvatarUploadPolicy.sampleSize(800, 600));
        assertEquals(4, AvatarUploadPolicy.sampleSize(6000, 4000));
        assertTrue(12000 / AvatarUploadPolicy.sampleSize(12000, 9000) <= 1600);
    }
    @Test public void resolvesStoredAndAbsoluteAvatarUrlsWithoutDuplicatingApiContext() {
        String base = "https://example.test/api/";
        assertEquals("https://example.test/api/uploads/a.jpg", AvatarUploadPolicy.resolveUrl(base, "/uploads/a.jpg"));
        assertEquals("https://example.test/api/uploads/a.jpg", AvatarUploadPolicy.resolveUrl(base, "uploads/a.jpg"));
        assertEquals("https://example.test/api/uploads/a.jpg", AvatarUploadPolicy.resolveUrl(base, "/api/uploads/a.jpg"));
        assertEquals("https://images.test/a.jpg", AvatarUploadPolicy.resolveUrl(base, "https://images.test/a.jpg"));
        assertNull(AvatarUploadPolicy.resolveUrl(base, " "));
        assertNull(AvatarUploadPolicy.resolveUrl(base, null));
    }
    @Test public void authenticatesOnlyAvatarReadsOnTheConfiguredServer() {
        String base = "https://example.test/api/";
        assertTrue(AvatarUploadPolicy.requiresAuthentication(base, "https://example.test/api/uploads/avatars/1_new.jpg"));
        assertFalse(AvatarUploadPolicy.requiresAuthentication(base, "https://images.test/api/uploads/avatars/a.jpg"));
        assertFalse(AvatarUploadPolicy.requiresAuthentication(base, "https://example.test:8443/api/uploads/avatars/a.jpg"));
        assertFalse(AvatarUploadPolicy.requiresAuthentication(base, "http://example.test/api/uploads/avatars/a.jpg"));
        assertFalse(AvatarUploadPolicy.requiresAuthentication(base, "https://example.test/api/user/me"));
        assertFalse(AvatarUploadPolicy.requiresAuthentication(base, null));
    }
}
