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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.maven.MavenCipher;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link MavenCipher}.
 * 
 * @since 3.0
 */
@Named
@Singleton
public class MavenCipherImpl
    implements MavenCipher
{
  private static final char SHIELD_BEGIN = '{';

  private static final char SHIELD_END = '}';

  private final PasswordCipher passwordCipher;

  @Inject
  public MavenCipherImpl(final CryptoHelper cryptoHelper) {
    this.passwordCipher = new PasswordCipher(cryptoHelper);
  }

  public String encrypt(final String str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    return SHIELD_BEGIN + doEncrypt(str, passPhrase) + SHIELD_END;
  }

  private String doEncrypt(final String str, final String passPhrase) {
    return new String(passwordCipher.encrypt(str.getBytes(Charsets.UTF_8), passPhrase), Charsets.UTF_8);
  }

  public String decrypt(final String str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    String payload = peel(str);
    checkArgument(payload != null, "Input string is not a password cipher");
    return doDecrypt(payload, passPhrase);
  }

  private String doDecrypt(final String str, final String passPhrase) {
    return new String(passwordCipher.decrypt(str.getBytes(Charsets.UTF_8), passPhrase), Charsets.UTF_8);
  }

  public boolean isPasswordCipher(final String str) {
    return peel(str) != null;
  }

  /**
   * Peels of the start and stop "shield" braces from payload if possible, otherwise returns {@code null} signaling that
   * input is invalid.
   */
  @Nullable
  private String peel(final String str) {
    if (Strings.isNullOrEmpty(str)) {
      return null;
    }
    int start = str.indexOf(SHIELD_BEGIN);
    int stop = str.indexOf(SHIELD_END);
    if (start != -1 && stop != -1 && stop > start + 1) {
      return str.substring(start + 1, stop);
    }
    return null;
  }
}
