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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.maven.MavenCipher;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.isNull;

/**
 * Default implementation of {@link MavenCipher}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class MavenCipherImpl
    extends ComponentSupport
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
    return new String(passwordCipher.encrypt(str.getBytes(StandardCharsets.UTF_8), passPhrase), StandardCharsets.UTF_8);
  }

  public String decrypt(final String str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    return doCipherCheck(str, passPhrase).orElse(str);
  }

  private String doDecrypt(final String str, final String passPhrase) {
    return new String(passwordCipher.decrypt(str.getBytes(StandardCharsets.UTF_8), passPhrase), StandardCharsets.UTF_8);
  }

  public boolean isPasswordCipher(final String str, final String passPhrase) {
    try {
      return doCipherCheck(str, passPhrase).isPresent();
    } catch (Exception ex) { // NOSONAR
      log.debug("Unable to decrypt input string");
      return false;
    }
  }

  /**
   * Helper to perform the peel and attempt a decrypt returning an empty optional or the
   * result of a successful decrypt call
   */
  @VisibleForTesting
  Optional<String> doCipherCheck(final String str, final String passPhrase) {
    //checks for existence of shields in input string
    return Optional.ofNullable(peel(str))
        .map(payload -> doDecrypt(payload, passPhrase));
  }

  /**
   * Peels of the start and stop "shield" braces from payload if possible, otherwise returns {@code null} signaling that
   * input is invalid.
   */
  private String peel(final String str) {
    if (isNullOrEmpty(str)) {
      return null;
    }
    int start = str.indexOf(SHIELD_BEGIN);
    int stop = str.lastIndexOf(SHIELD_END);
    if (start == 0 && stop == str.length()-1 && stop > start + 1) {
      return str.substring(start + 1, stop);
    }

    return null;
  }
}
