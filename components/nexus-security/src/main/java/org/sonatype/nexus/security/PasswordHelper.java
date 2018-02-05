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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.maven.MavenCipher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Password encryption helper.
 */
@Singleton
@Named
public class PasswordHelper
    extends ComponentSupport
{
  private static final String ENC = "CMMDwoV";

  private final MavenCipher mavenCipher;

  private final PhraseService phraseService;

  @Inject
  public PasswordHelper(final MavenCipher mavenCipher, final PhraseService phraseService) {
    this.mavenCipher = checkNotNull(mavenCipher);
    this.phraseService = checkNotNull(phraseService);
  }

  @Nullable
  public String encrypt(@Nullable final String password) {
    String encodedPassword = encrypt(password, phraseService.getPhrase(ENC));
    if (encodedPassword != null && !encodedPassword.equals(password)) {
      return phraseService.mark(encodedPassword);
    }
    return encodedPassword;
  }

  @Nullable
  public String encrypt(@Nullable final String password, final String encoding) {
    if (password == null) {
      return null;
    }
    // check the input is not already encrypted
    if (mavenCipher.isPasswordCipher(password)) {
      log.warn("Value appears to be already encrypted", log.isDebugEnabled() ? new IllegalArgumentException() : null);
      return password;
    }
    return mavenCipher.encrypt(password, encoding);
  }

  @Nullable
  public String decrypt(@Nullable final String encodedPassword) {
    if (phraseService.usesLegacyEncoding(encodedPassword)) {
      return decrypt(encodedPassword, ENC);
    }
    return decrypt(encodedPassword, phraseService.getPhrase(ENC));
  }

  @Nullable
  public String decrypt(@Nullable final String encodedPassword, final String encoding) {
    if (encodedPassword == null) {
      return null;
    }
    // check the input is encrypted
    if (!mavenCipher.isPasswordCipher(encodedPassword)) {
      log.warn("Value appears to be already decrypted", log.isDebugEnabled() ? new IllegalArgumentException() : null);
      return encodedPassword;
    }
    return mavenCipher.decrypt(encodedPassword, encoding);
  }

  /**
   * Attempt to decrypt the given input; returns the original input if it can't be decrypted.
   *
   * @since 3.8
   */
  @Nullable
 public String tryDecrypt(@Nullable final String encodedPassword) {
   try {
     return decrypt(encodedPassword);
   }
   catch (RuntimeException e) {
     log.warn("Failed to decrypt value, loading as plain text", log.isDebugEnabled() ? e : null);
     return encodedPassword;
   }
 }
}
