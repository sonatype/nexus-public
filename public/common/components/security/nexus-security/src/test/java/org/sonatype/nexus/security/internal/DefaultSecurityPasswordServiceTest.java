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

import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;

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
        hashingHandlerFactory);
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
}
