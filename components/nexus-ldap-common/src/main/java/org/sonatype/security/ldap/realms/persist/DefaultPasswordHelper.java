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
package org.sonatype.security.ldap.realms.persist;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.ldap.upgrade.cipher.PlexusCipher;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipherException;

import static com.google.common.base.Preconditions.checkNotNull;


@Singleton
@Named
public class DefaultPasswordHelper
    implements PasswordHelper
{

  private static final String ENC = "CMMDwoV";

  private final PlexusCipher plexusCipher;

  private final String masterPhrase = System.getProperty("nexus.security.masterPhrase", ENC);

  @Inject
  public DefaultPasswordHelper(final PlexusCipher plexusCipher) {
    this.plexusCipher = checkNotNull(plexusCipher);
  }

  public String encrypt(String password)
      throws PlexusCipherException
  {
    if (password != null) {
      String result = plexusCipher.encrypt(password, masterPhrase);
      // decryptDecorated will ignore anything outside these braces
      return ENC.equals(masterPhrase) ? result : "~{" + result + "}~";
    }
    return null;
  }

  public String decrypt(String encodedPassword)
      throws PlexusCipherException
  {
    if (encodedPassword != null) {
      if (!ENC.equals(masterPhrase) && encodedPassword.contains("~{")) {
        return plexusCipher.decryptDecorated(encodedPassword, masterPhrase);
      }
      return plexusCipher.decrypt(encodedPassword, ENC);
    }
    return null;
  }
}
