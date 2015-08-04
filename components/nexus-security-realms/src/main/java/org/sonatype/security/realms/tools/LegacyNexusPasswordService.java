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
package org.sonatype.security.realms.tools;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.format.HexFormat;

/*
 * PasswordService for handling legacy passwords (SHA-1 and MD5)
 * 
 * @since 3.1
 */
@Singleton
@Typed(PasswordService.class)
@Named("legacy")
public class LegacyNexusPasswordService
    implements PasswordService
{
  DefaultPasswordService sha1PasswordService;

  DefaultPasswordService md5PasswordService;

  public LegacyNexusPasswordService() {
    //Initialize and configure sha1 password service
    this.sha1PasswordService = new DefaultPasswordService();
    DefaultHashService sha1HashService = new DefaultHashService();
    sha1HashService.setHashAlgorithmName("SHA-1");
    sha1HashService.setHashIterations(1);
    sha1HashService.setGeneratePublicSalt(false);
    this.sha1PasswordService.setHashService(sha1HashService);
    this.sha1PasswordService.setHashFormat(new HexFormat());

    //Initialize and configure md5 password service
    this.md5PasswordService = new DefaultPasswordService();
    DefaultHashService md5HashService = new DefaultHashService();
    md5HashService.setHashAlgorithmName("MD5");
    md5HashService.setHashIterations(1);
    md5HashService.setGeneratePublicSalt(false);
    this.md5PasswordService.setHashService(md5HashService);
    this.md5PasswordService.setHashFormat(new HexFormat());
  }

  @Override
  public String encryptPassword(Object plaintextPassword)
      throws IllegalArgumentException
  {
    throw new IllegalArgumentException("Not supported");
  }

  @Override
  public boolean passwordsMatch(Object submittedPlaintext, String encrypted) {
    //Legacy passwords can be hashed with sha-1 or md5, check both

    if (this.sha1PasswordService.passwordsMatch(submittedPlaintext, encrypted)) {
      return true;
    }

    if (this.md5PasswordService.passwordsMatch(submittedPlaintext, encrypted)) {
      return true;
    }

    return false;
  }
}
