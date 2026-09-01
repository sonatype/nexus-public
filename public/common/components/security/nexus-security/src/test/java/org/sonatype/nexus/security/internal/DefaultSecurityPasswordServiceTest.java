/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.security.internal;

import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;

import com.google.common.base.Ticker;
import org.apache.shiro.crypto.hash.Hash;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultSecurityPasswordService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultSecurityPasswordServiceTest
{
  private DefaultSecurityPasswordService underTest;

  @Mock
  private HashingHandlerFactory hashingHandlerFactory;

  @Mock
  private HashingHandler hashingHandler;

  @Before
  public void setUp() throws Exception {
    underTest = new DefaultSecurityPasswordService(new LegacyNexusPasswordService(), "shiro1", null,
        hashingHandlerFactory, true, 1000, 60);
    when(hashingHandlerFactory.create(any(String.class), any(byte[].class), any())).thenReturn(hashingHandler);
    when(hashingHandlerFactory.create(any(String.class))).thenReturn(hashingHandler);
  }

  @Test(expected = CipherException.class)
  public void testPasswordsMatch_whenVerifyThrowsCipherException() throws InvalidKeySpecException {
    String password = "admin123";
    String sha1Hash = "f865b53623b121fd34ee5426c792e5c33af8c227";

    when(hashingHandler.verify(any(char[].class), eq(sha1Hash))).thenThrow(CipherException.class);

    underTest.passwordsMatch(password, sha1Hash);
  }

  @Test
  public void testSha1Hash() throws InvalidKeySpecException {
    String password = "admin123";
    String sha1Hash = "f865b53623b121fd34ee5426c792e5c33af8c227";

    when(hashingHandler.verify(any(char[].class), eq(sha1Hash))).thenThrow(IllegalArgumentException.class);
    assertThat(underTest.passwordsMatch(password, sha1Hash), is(true));

    when(hashingHandler.verify(any(char[].class), eq(sha1Hash))).thenThrow(NullPointerException.class);
    assertThat(underTest.passwordsMatch(password, sha1Hash), is(true));
  }

  @Test
  public void testMd5Hash() throws InvalidKeySpecException {
    String password = "admin123";
    String md5Hash = "0192023a7bbd73250516f069df18b500";

    when(hashingHandler.verify(any(char[].class), eq(md5Hash))).thenThrow(IllegalArgumentException.class);
    assertThat(underTest.passwordsMatch(password, md5Hash), is(true));

    when(hashingHandler.verify(any(char[].class), eq(md5Hash))).thenThrow(NullPointerException.class);
    assertThat(underTest.passwordsMatch(password, md5Hash), is(true));
  }

  @Test
  public void testShiro1HashFormat() throws InvalidKeySpecException {
    String password = "admin123";
    String shiro1Hash =
        "$shiro1$SHA-512$1024$zjU1u+Zg9UNwuB+HEawvtA==$IzF/OWzJxrqvB5FCe/2+UcZhhZYM2pTu0TEz7Ybnk65AbbEdUk9ntdtBzkN8P3gZby2qz6MHKqAe8Cjai9c4Gg==";

    when(hashingHandler.verify(any(char[].class), eq(shiro1Hash))).thenThrow(IllegalArgumentException.class);
    assertThat(underTest.passwordsMatch(password, shiro1Hash), is(true));

    when(hashingHandler.verify(any(char[].class), eq(shiro1Hash))).thenThrow(NullPointerException.class);
    assertThat(underTest.passwordsMatch(password, shiro1Hash), is(true));
  }

  @Test
  public void testInvalidSha1Hash() {
    String password = "admin123";
    String sha1Hash = "f865b53623b121fd34ee5426c792e5c33af8c228";

    assertThat(underTest.passwordsMatch(password, sha1Hash), is(false));
  }

  @Test
  public void testInvalidMd5Hash() {
    String password = "admin123";
    String md5Hash = "0192023a7bbd73250516f069df18b501";

    assertThat(underTest.passwordsMatch(password, md5Hash), is(false));
  }

  @Test
  public void testInvalidShiro1HashFormat() {
    String password = "admin123";
    String shiro1Hash =
        "$shiro1$SHA-512$1024$zjU1u+Zg9UNwuB+HEawvtA==$IzF/OWzjxrqvB5FCe/2+UcZhhZYM2pTu0TEz7Ybnk65AbbEdUk9ntdtBzkN8P3gZby2qz6MHKqAe8Cjai9c4Gg==";

    assertThat(underTest.passwordsMatch(password, shiro1Hash), is(false));
  }

  @Test
  public void testHash() {
    String password = "testpassword";
    Hash hash = underTest.hashPassword(password);

    assertThat(underTest.passwordsMatch(password, hash), is(true));
  }

  @Test
  public void testMalformedFipsHash() {
    String password = "admin123";
    String badHash = "$PBKDF2WithHmacSHA256$i=notanumber$salt$hash";
    assertThat(underTest.passwordsMatch(password, badHash), is(false));
  }

  @Test
  public void testInvalidBase64FipsHash() {
    String password = "admin123";
    String badHash = "$PBKDF2WithHmacSHA256$i=10000$invalid$hash";
    assertThat(underTest.passwordsMatch(password, badHash), is(false));
  }

  @Test
  public void testSuccessfulVerificationIsCached() throws InvalidKeySpecException {
    String password = "admin123";
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(true);

    assertThat(underTest.passwordsMatch(password, hash), is(true));
    assertThat(underTest.passwordsMatch(password, hash), is(true));

    // Second identical request is served from the cache; the (expensive) verify runs only once.
    verify(hashingHandler, times(1)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testFailedVerificationIsNotCached() throws InvalidKeySpecException {
    String password = "wrong";
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(false);

    assertThat(underTest.passwordsMatch(password, hash), is(false));
    assertThat(underTest.passwordsMatch(password, hash), is(false));

    // Failures must keep paying the full hashing cost so brute-force guessing stays throttled.
    verify(hashingHandler, times(2)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testCacheKeyIncludesSubmittedPassword() throws InvalidKeySpecException {
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(true);

    assertThat(underTest.passwordsMatch("passwordOne", hash), is(true));
    assertThat(underTest.passwordsMatch("passwordTwo", hash), is(true));

    // A different submitted password must not hit the first password's cache entry.
    verify(hashingHandler, times(2)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testCacheKeyIncludesStoredHash() throws InvalidKeySpecException {
    String password = "admin123";
    String hashBeforeChange = "$PBKDF2WithHmacSHA256$i=10000$c2FsdC1vbmU=$aGFzaC1vbmU=";
    String hashAfterChange = "$PBKDF2WithHmacSHA256$i=10000$c2FsdC10d28=$aGFzaC10d28=";
    when(hashingHandler.verify(any(char[].class), eq(hashBeforeChange))).thenReturn(true);
    when(hashingHandler.verify(any(char[].class), eq(hashAfterChange))).thenReturn(true);

    assertThat(underTest.passwordsMatch(password, hashBeforeChange), is(true));
    // A changed password produces a new stored hash, so the prior entry must not be reused.
    assertThat(underTest.passwordsMatch(password, hashAfterChange), is(true));

    verify(hashingHandler, times(1)).verify(any(char[].class), eq(hashBeforeChange));
    verify(hashingHandler, times(1)).verify(any(char[].class), eq(hashAfterChange));
  }

  @Test
  public void testVerificationNotCachedWhenCacheDisabled() throws InvalidKeySpecException {
    DefaultSecurityPasswordService noCache = new DefaultSecurityPasswordService(new LegacyNexusPasswordService(),
        "shiro1", null, hashingHandlerFactory, false, 1000, 60);
    String password = "admin123";
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(true);

    assertThat(noCache.passwordsMatch(password, hash), is(true));
    assertThat(noCache.passwordsMatch(password, hash), is(true));

    verify(hashingHandler, times(2)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testUnicodePasswordCachingIsKeyedCorrectly() throws InvalidKeySpecException {
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(true);

    // Multi-byte + surrogate-pair (emoji) password: repeating it must hit the cache (UTF-8 round-trips stably).
    assertThat(underTest.passwordsMatch("p\u00e9\u4e2d\ud83d\ude00", hash), is(true));
    assertThat(underTest.passwordsMatch("p\u00e9\u4e2d\ud83d\ude00", hash), is(true));
    verify(hashingHandler, times(1)).verify(any(char[].class), eq(hash));

    // A different Unicode password (differs only in the final emoji) must NOT collide on the same cache key.
    assertThat(underTest.passwordsMatch("p\u00e9\u4e2d\ud83d\ude01", hash), is(true));
    verify(hashingHandler, times(2)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testCachedSuccessExpiresAfterConfiguredTtl() throws InvalidKeySpecException {
    MutableTicker ticker = new MutableTicker();
    DefaultSecurityPasswordService underTestWithTtl = new DefaultSecurityPasswordService(
        new LegacyNexusPasswordService(), "shiro1", null, hashingHandlerFactory, true, 1000, 2, ticker);
    String password = "admin123";
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    when(hashingHandler.verify(any(char[].class), eq(hash))).thenReturn(true);

    assertThat(underTestWithTtl.passwordsMatch(password, hash), is(true)); // verify #1, cached
    assertThat(underTestWithTtl.passwordsMatch(password, hash), is(true)); // served from cache
    verify(hashingHandler, times(1)).verify(any(char[].class), eq(hash));

    ticker.advanceSeconds(3); // exceed the 2s expireAfterWrite

    assertThat(underTestWithTtl.passwordsMatch(password, hash), is(true)); // entry expired -> verify #2
    verify(hashingHandler, times(2)).verify(any(char[].class), eq(hash));
  }

  @Test
  public void testNullSubmittedPasswordReturnsFalse() {
    String hash = "$PBKDF2WithHmacSHA256$i=10000$c2FsdHNhbHQ=$aGFzaGhhc2g=";
    assertThat(underTest.passwordsMatch(null, hash), is(false));
    // Early return before any hashing/cache work.
    verifyNoInteractions(hashingHandler);
  }

  @Test
  public void testNullStoredHashReturnsFalse() {
    assertThat(underTest.passwordsMatch("admin123", (String) null), is(false));
    verifyNoInteractions(hashingHandler);
  }

  @Test
  public void testHashOverloadBypassesCredentialCache() {
    // The Hash-accepting overload delegates straight to Shiro; it never consults or populates the
    // string-keyed verified-credentials cache (nor the Nexus HashingHandler path), so repeated calls are
    // independent. This documents that intentional behaviour for future maintainers.
    String password = "testpassword";
    Hash hash = underTest.hashPassword(password);

    assertThat(underTest.passwordsMatch(password, hash), is(true));
    assertThat(underTest.passwordsMatch(password, hash), is(true));

    verifyNoInteractions(hashingHandler);
  }

  /**
   * Manually-advanced {@link Ticker} so cache-expiry behaviour can be tested deterministically without sleeping.
   */
  private static final class MutableTicker
      extends Ticker
  {
    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    void advanceSeconds(final long seconds) {
      nanos += TimeUnit.SECONDS.toNanos(seconds);
    }
  }
}
