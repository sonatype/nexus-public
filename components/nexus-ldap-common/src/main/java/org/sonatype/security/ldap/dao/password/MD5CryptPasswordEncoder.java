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
package org.sonatype.security.ldap.dao.password;

import java.security.NoSuchAlgorithmException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.ldap.dao.password.hash.MD5Crypt;


/**
 * @author cstamas
 */
@Singleton
@Named("crypt")
public class MD5CryptPasswordEncoder
    implements PasswordEncoder
{
  final private MD5Crypt md5Crypt = new MD5Crypt();

  @Override
  public String getMethod() {
    return "CRYPT";
  }

  @Override
  public String encodePassword(String password, Object salt) {
    try {
      return "{CRYPT}" + md5Crypt.crypt(password);
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("No MD5 Algorithm", e);
    }
  }

  @Override
  public boolean isPasswordValid(String encPassword, String inputPassword, Object salt) {
    try {
      String encryptedPassword = encPassword;
      if (encryptedPassword.startsWith("{crypt}") || encryptedPassword.startsWith("{CRYPT}")) {
        encryptedPassword = encryptedPassword.substring("{crypt}".length());
      }

      int lastDollar = encryptedPassword.lastIndexOf('$');
      String realSalt = encryptedPassword.substring("$1$".length(), lastDollar);

      String check = md5Crypt.crypt(inputPassword, realSalt);

      return check.equals(encryptedPassword);
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("No MD5 Algorithm", e);
    }
  }

}
