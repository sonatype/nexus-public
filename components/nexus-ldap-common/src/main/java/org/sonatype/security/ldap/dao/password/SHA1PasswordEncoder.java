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

import org.apache.shiro.codec.Base64;

/**
 * @author cstamas
 */
@Singleton
@Named("sha")
public class SHA1PasswordEncoder
    implements PasswordEncoder
{

  @Named
  public String getMethod() {
    return "SHA";
  }

  @Named
  public String encodePassword(String password, Object salt) {
    return "{SHA}" + encodeString(password);
  }

  @Named
  public boolean isPasswordValid(String encPassword, String inputPassword, Object salt) {
    String encryptedPassword = encPassword;
    if (encryptedPassword.startsWith("{SHA}") || encryptedPassword.startsWith("{sha}")) {
      encryptedPassword = encryptedPassword.substring("{sha}".length());
    }

    String check = encodePassword(inputPassword, salt).substring("{sha}".length());

    return check.equals(encryptedPassword);
  }

  protected String encodeString(String input) {
    InputStream is = new ByteArrayInputStream(input.getBytes());
    String result = null;
    try {
      byte[] buffer = new byte[1024];
      MessageDigest md = MessageDigest.getInstance("SHA1");
      int numRead;
      do {
        numRead = is.read(buffer);
        if (numRead > 0) {
          md.update(buffer, 0, numRead);
        }
      }
      while (numRead != -1);
      result = Base64.encodeToString(md.digest());
    }
    catch (Exception e) {
    }
    return result;
  }

}
