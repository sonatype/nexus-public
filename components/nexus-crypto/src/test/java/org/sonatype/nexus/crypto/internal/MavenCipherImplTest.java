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
package org.sonatype.nexus.crypto.internal;

import java.nio.CharBuffer;
import java.security.Security;

import org.sonatype.goodies.testsupport.TestSupport;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * UT for {@link MavenCipherImpl}.
 */
public class MavenCipherImplTest
    extends TestSupport
{
  private static final String passPhrase = "foofoo";

  private static final String plaintext = "my testing phrase";

  private static final String plaintext_mixed = "{" + plaintext + "}";

  private static final String plaintext_spec_char_xtrabracket = plaintext_mixed + "}";

  private static final String plaintext_random = "{specialpass word ][4^$$}}";

  private static final String plaintext_special_char = "{.}-";

  private static final String plaintext_one_bracket = "{CFUju8n8eKQHj8u0HI9uQMRm";

  private static final String encrypted = "{5FjvnZvhNDMHHnxXoPu1a0WcgZzaArKRCnGBnsA83R7rYQHKGFrprtAM4Qyr4diV}";

  private static final String plaintext_mixed_encrypted = "{b9Xrnp7OFSUHmJ09eD5CA+dpbHnAHepZNJOVeR7SPiDTZ0kHFSvQLpiQolqJuHWO}";

  private static final String plaintext_special_encrypted = "{ggrGm3B7H4QH0cJbjfEle2b5b3Lp7WvFEBUadBSK764=}";

  private MavenCipherImpl testSubject;

  @Before
  public void prepare() {
    Security.addProvider(new BouncyCastleProvider());
    testSubject = new MavenCipherImpl(new CryptoHelperImpl());
  }

  @After
  public void cleanup() {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
  }

  @Test
  public void payloadDetection() throws Exception {
    assertThat(testSubject.isPasswordCipher(null), is(false));
    assertIsPasswordCipher("", false);
    assertIsPasswordCipher("{}", false);
    assertIsPasswordCipher("{ }", false);
    assertIsPasswordCipher("{ {} }", false);
    assertIsPasswordCipher(plaintext, false);
    assertIsPasswordCipher(plaintext_mixed, false);
    assertIsPasswordCipher(plaintext_spec_char_xtrabracket, false);
    assertIsPasswordCipher(plaintext_random, false);
    assertIsPasswordCipher(plaintext_special_char, false);
    assertIsPasswordCipher(plaintext_one_bracket, false);
    assertIsPasswordCipher(encrypted, true);
    assertIsPasswordCipher(plaintext_mixed_encrypted, true);
    assertIsPasswordCipher(plaintext_special_encrypted, true);
  }

  // test both string and char array equivalent
  private void assertIsPasswordCipher(final String str, final boolean expected) {
    assertThat(testSubject.isPasswordCipher(str), is(expected));
    char[] chars = str.toCharArray();
    assertThat(testSubject.isPasswordCipher(CharBuffer.wrap(chars)), is(expected));
  }

  @Test
  public void encrypt() throws Exception {
    String enc = testSubject.encrypt(plaintext, passPhrase);
    assertThat(enc, notNullValue());
  }

  @Test
  public void encrypt_with_special_chars() throws Exception {
    String mixed = testSubject.encrypt(plaintext_mixed, passPhrase);
    System.out.println(mixed);
    assertThat(mixed, notNullValue());

    String xtraSpecChar = testSubject.encrypt(plaintext_spec_char_xtrabracket, passPhrase);
    System.out.println(xtraSpecChar);
    assertThat(xtraSpecChar, notNullValue());

    String randomChars = testSubject.encrypt(plaintext_random, passPhrase);
    System.out.println(randomChars);
    assertThat(randomChars, notNullValue());

    String specialChar = testSubject.encrypt(plaintext_special_char, passPhrase);
    System.out.println(specialChar);
    assertThat(specialChar, notNullValue());
  }

  @Test
  public void decrypt_withSpecialChars() throws Exception {
    String specChar = testSubject.decrypt(plaintext_mixed_encrypted, passPhrase);
    assertThat(specChar, equalTo(plaintext_spec_char_xtrabracket));

    String minimalCase = testSubject.decrypt(plaintext_special_encrypted, passPhrase);
    assertThat(minimalCase, equalTo(plaintext_special_char));
  }

  @Test (expected = IllegalArgumentException.class)
  public void decrypt_NonEncrypted_with_Brackets() throws Exception {
    testSubject.decrypt(plaintext_mixed, passPhrase);
  }

  @Test
  public void decrypt() throws Exception {
    String dec = testSubject.decrypt(encrypted, passPhrase);
    assertThat(dec, equalTo(plaintext));
  }

  @Test(expected = IllegalArgumentException.class)
  public void decryptCorruptedMissingEnd() throws Exception {
    testSubject.decrypt(plaintext_one_bracket, passPhrase);
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
    assertRoundTrip(plaintext);
  }

  @Test
  public void roundTrip_WithSpecialChars() throws Exception {
    assertRoundTrip(plaintext_random);
  }

  // test both string and char array equivalent
  private void assertRoundTrip(final String expected) {
    String actual = testSubject.decrypt(testSubject.encrypt(expected, passPhrase), passPhrase);
    assertThat(actual, equalTo(expected));

    char[] expectedChars = expected.toCharArray();
    char[] actualChars = testSubject.decryptChars(
        testSubject.encrypt(CharBuffer.wrap(expectedChars), passPhrase), passPhrase);

    assertThat(actualChars, equalTo(expectedChars));
  }

  /**
   * This is a "master password" string created using Maven 3.0.4 on CLI as described here:
   * http://maven.apache.org/guides/mini/guide-encryption.html#How_to_create_a_master_password
   */
  @Test
  public void masterPasswordCreatedWithMaven304Cli() {
    String passPhrase = "settings.security";
    String plaintext = "123321";
    String encrypted = "{KW5k/vol4xMHusz6ikZqdj0t4YRClp4/5Dsb30+M9R0=}";
    assertThat(testSubject.decrypt(encrypted, passPhrase), equalTo(plaintext));
  }

  /**
   * This is a "master password" string created using Maven 3.2.2 on CLI as described here:
   * http://maven.apache.org/guides/mini/guide-encryption.html#How_to_create_a_master_password
   */
  @Test
  public void masterPasswordCreatedWithMaven322Cli() {
    String passPhrase = "settings.security";
    String plaintext = "123321";
    String encrypted = "{eO8Yc66/I/IHaeg4CoF+/o5bwS5IIyfWcgsYhS0s9W8=}";
    assertThat(testSubject.decrypt(encrypted, passPhrase), equalTo(plaintext));
  }

  @Test
  public void cipherMethodsAreFreeFromSideEffects() {
    CharBuffer charBuffer = CharBuffer.wrap(plaintext_random);

    assertThat(plaintext_random.contentEquals(charBuffer), is(true));
    testSubject.isPasswordCipher(charBuffer);
    assertThat(plaintext_random.contentEquals(charBuffer), is(true));
    testSubject.encrypt(charBuffer, passPhrase);
    assertThat(plaintext_random.contentEquals(charBuffer), is(true));
  }
}
