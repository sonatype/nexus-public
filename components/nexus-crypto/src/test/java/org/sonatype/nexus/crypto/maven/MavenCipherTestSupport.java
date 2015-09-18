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
package org.sonatype.nexus.crypto.maven;

import java.security.Security;

import org.sonatype.goodies.testsupport.TestSupport;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * UT support for {@link MavenCipher} implementations containing simple set of excersises.
 */
public abstract class MavenCipherTestSupport
    extends TestSupport
{
  protected final String passPhrase;

  protected final String plaintext;

  protected final String encrypted;

  protected final MavenCipher testSubject;

  protected MavenCipherTestSupport(final String passPhrase, final String plaintext, final String encrypted,
      final MavenCipher testSubject)
  {
    this.passPhrase = checkNotNull(passPhrase);
    this.plaintext = checkNotNull(plaintext);
    this.encrypted = checkNotNull(encrypted);
    this.testSubject = checkNotNull(testSubject);
  }

  @Before
  public void prepare() {
    Security.addProvider(new BouncyCastleProvider());
  }

  @After
  public void cleanup() {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
  }

  @Test
  public void payloadDetection() {
    assertThat(testSubject.isPasswordCipher(plaintext), is(false));
    assertThat(testSubject.isPasswordCipher(""), is(false));
    assertThat(testSubject.isPasswordCipher("{}"), is(false));
    assertThat(testSubject.isPasswordCipher(null), is(false));
    assertThat(testSubject.isPasswordCipher(encrypted), is(true));
    assertThat(testSubject.isPasswordCipher("{ }"), is(true));
  }

  @Test
  public void encrypt() throws Exception {
    String enc = testSubject.encrypt(plaintext, passPhrase);
    assertThat(enc, notNullValue());
  }

  @Test
  public void decrypt() throws Exception {
    String dec = testSubject.decrypt(encrypted, passPhrase);
    assertThat(dec, equalTo(plaintext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void decryptCorruptedMissingEnd() throws Exception {
    testSubject.decrypt("{CFUju8n8eKQHj8u0HI9uQMRm", passPhrase);
  }

  @Test(expected = NullPointerException.class)
  public void decryptNull() throws Exception {
    testSubject.decrypt(null, passPhrase);
  }

  @Test(expected = NullPointerException.class)
  public void decryptNullPassPhrase() throws Exception {
    testSubject.decrypt(encrypted, null);
  }

  @Test
  public void roundTrip() throws Exception {
    String dec = testSubject.decrypt(testSubject.encrypt(plaintext, passPhrase), passPhrase);
    assertThat(dec, equalTo(plaintext));
  }
}