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
package org.sonatype.nexus.security;

import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicReference;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;
import org.sonatype.nexus.crypto.internal.MavenCipherImpl;

import com.google.common.base.Throwables;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.security.PhraseService.LEGACY_PHRASE_SERVICE;

/**
 * UT for {@link PasswordHelper}.
 * 
 * @since 2.8.0
 */
public class PasswordHelperTest
    extends TestSupport
{
  private PasswordHelper legacyPasswordHelper;

  private PasswordHelper customPasswordHelper;

  @Before
  public void init() throws Exception {
    legacyPasswordHelper = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()), LEGACY_PHRASE_SERVICE);
    customPasswordHelper = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()), new AbstractPhraseService(true)
    {
      @Override
      protected String getMasterPhrase() {
        return "sterces, sterces, sterces";
      }
    });
  }

  @Test
  public void testEncrypt_NullInput() throws Exception {
    assertThat(legacyPasswordHelper.encrypt(null), is(nullValue()));
    assertThat(customPasswordHelper.encrypt(null), is(nullValue()));
  }

  @Test
  public void testEncrypt_EmptyInput() throws Exception {
    assertThat(legacyPasswordHelper.encrypt(""), allOf(startsWith("{"), endsWith("}")));
    assertThat(customPasswordHelper.encrypt(""), allOf(startsWith("~{"), endsWith("}~")));
  }

  @Test
  public void testEncrypt_PlainInput() throws Exception {
    assertThat(legacyPasswordHelper.encrypt("test"), allOf(startsWith("{"), endsWith("}")));
    assertThat(customPasswordHelper.encrypt("test"), allOf(startsWith("~{"), endsWith("}~")));
  }

  @Test
  public void testEncrypt_AlreadyEncryptedInput() throws Exception {
    assertThat(legacyPasswordHelper.encrypt("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"), is("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"));
    assertThat(customPasswordHelper.encrypt("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"), is("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"));
  }

  @Test
  public void testEncrypt_StringIncludingShields() throws Exception {
    //check the resultant value is protected by braces and has been encrypted (not equal to the input string)
    assertThat(legacyPasswordHelper.encrypt("{test}"), allOf(startsWith("{"), endsWith("}"), not("{test}")));
    assertThat(customPasswordHelper.encrypt("{test}"), allOf(startsWith("~{"), endsWith("}~"), not("~{test}~")));
  }

  @Test
  public void testDecrypt_NullInput() throws Exception {
    assertThat(legacyPasswordHelper.decrypt(null), is(nullValue()));
    assertThat(customPasswordHelper.decrypt(null), is(nullValue()));
  }

  @Test
  public void testDecrypt_EmptyInput() throws Exception {
    assertThat(legacyPasswordHelper.decrypt(""), is(""));
    assertThat(customPasswordHelper.decrypt(""), is(""));
  }

  @Test
  public void testDecrypt_EncryptedInput() throws Exception {
    assertThat(legacyPasswordHelper.decrypt("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"), is("test"));
    assertThat(customPasswordHelper.decrypt("{X4bkkyyxOxkH+JFw6vVV3Gp0ONzT0aSzGOUCSSH+P5E=}"), is("test"));
  }

  @Test
  public void testDecrypt_AlreadyDecryptedInput() throws Exception {
    assertThat(legacyPasswordHelper.decrypt("test"), is("test"));
    assertThat(customPasswordHelper.decrypt("test"), is("test"));
  }

  @Test
  public void testThreadSafety() throws Exception {
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    final String password = "just-some-password-for-testing";
    Thread[] threads = new Thread[20];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread()
      {
        @Override
        public void run() {
          for (int i = 0; i < 20; i++) {
            try {
              MatcherAssert.assertThat(legacyPasswordHelper.decrypt(legacyPasswordHelper.encrypt(password)), is(password));
              MatcherAssert.assertThat(customPasswordHelper.decrypt(customPasswordHelper.encrypt(password)), is(password));
            }
            catch (Throwable e) {
              error.compareAndSet(null, e);
            }
          }
        }
      };
    }
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      thread.join();
    }
    if (error.get() != null) {
      Throwables.throwIfUnchecked(error.get());
      throw new RuntimeException(error.get());
    }
  }

  @Test
  public void testCustomMasterPhrase() throws Exception {
    String password = "clear-text-password";
    String encodedPass = customPasswordHelper.encrypt(password);

    try {
      legacyPasswordHelper.decrypt(encodedPass);
      fail("Expected RuntimeException wrapping GeneralSecurityException");
    }
    catch (RuntimeException e) {
      assertThat(e.getCause(), is(instanceOf(GeneralSecurityException.class)));
    }

    assertThat(customPasswordHelper.decrypt(encodedPass), is(password));
  }

  @Test
  public void testLegacyPhraseFallback() throws Exception {
    String password = "clear-text-password";
    String encodedPass = legacyPasswordHelper.encrypt(password);

    assertThat(legacyPasswordHelper.decrypt(encodedPass), is(password));

    // should still work by falling back to legacy pass-phrase
    assertThat(customPasswordHelper.decrypt(encodedPass), is(password));
  }

  @Test
  public void testCustomPhraseFile() throws Exception {
    PhraseService phraseService = new FilePhraseService(util.resolveFile("target/test-classes/custom.enc"));
    PasswordHelper underTest = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()), phraseService);

    String password = "clear-text-password";
    String encodedPass = underTest.encrypt(password);

    assertThat(underTest.decrypt(encodedPass), is(password));
  }

  @Test
  public void testMissingPhraseFile() throws Exception {
    PhraseService phraseService = new FilePhraseService(util.resolveFile("target/test-classes/missing.enc"));
    PasswordHelper underTest = new PasswordHelper(new MavenCipherImpl(new CryptoHelperImpl()), phraseService);

    String password = "clear-text-password";
    try {
      underTest.encrypt(password);
      fail("Expected RuntimeException wrapping FileNotFoundException");
    }
    catch (RuntimeException e) {
      assertThat(e.getCause(), is(instanceOf(FileNotFoundException.class)));
    }
  }
}
