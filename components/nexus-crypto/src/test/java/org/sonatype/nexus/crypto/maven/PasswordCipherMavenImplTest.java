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

import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * UT for {@link MavenCipher} with {@link PasswordCipherMavenImpl}
 */
public class PasswordCipherMavenImplTest
    extends MavenCipherTestSupport
{
  private static final String passPhrase = "foofoo";

  private static final String plaintext = "my testing phrase";

  private static final String encrypted = "{5FjvnZvhNDMHHnxXoPu1a0WcgZzaArKRCnGBnsA83R7rYQHKGFrprtAM4Qyr4diV}";

  public PasswordCipherMavenImplTest() {
    super(passPhrase, plaintext, encrypted, new MavenCipher(new PasswordCipherMavenImpl(new CryptoHelperImpl())));
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
}