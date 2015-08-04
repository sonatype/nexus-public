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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.codec.Hex;

/**
 * @author cstamas
 */
@Singleton
@Named("md5")
public class MD5PasswordEncoder
    implements PasswordEncoder
{

  @Override
  public String getMethod() {
    return "MD5";
  }

  @Override
  public String encodePassword(String password, Object salt) {
    return "{MD5}" + encodeString(password);
  }

  @Override
  public boolean isPasswordValid(String encPassword, String inputPassword, Object salt) {
    String encryptedPassword = this.stripHeader(encPassword);
    String check = this.stripHeader(encodePassword(inputPassword, salt));

    return check.equals(encryptedPassword);
  }

  protected String stripHeader(String encryptedPassword) {
    if (encryptedPassword.startsWith("{" + getMethod().toUpperCase() + "}")
        || encryptedPassword.startsWith("{" + getMethod().toLowerCase() + "}")) {
      encryptedPassword = encryptedPassword.substring("{MD5}".length());
    }
    return encryptedPassword;
  }


  protected String encodeString(String input) {
    InputStream is = new ByteArrayInputStream(input.getBytes());
    String result = null;
    try {
      byte[] buffer = new byte[1024];
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      int numRead;
      do {
        numRead = is.read(buffer);
        if (numRead > 0) {
          md5.update(buffer, 0, numRead);
        }
      }
      while (numRead != -1);
      result = new String(Hex.encode(md5.digest()));
    }
    catch (Exception e) {
    }
    return result;
  }

}
