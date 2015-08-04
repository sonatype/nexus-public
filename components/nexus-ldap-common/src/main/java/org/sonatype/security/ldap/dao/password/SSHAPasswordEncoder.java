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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.codec.Base64;

/**
 * @author bdemers
 */
@Singleton
@Named("ssha")
public class SSHAPasswordEncoder
    implements PasswordEncoder
{
  private static final String SSHA_PREFIX = "{SSHA}";

  private final Random random = new Random();

  @Override
  public String getMethod() {
    return "SSHA";
  }

  @Override
  public String encodePassword(String password, Object salt) {
    try {
      byte[] saltBytes = null;

      // needs to be null, byteArray, or a String
      if (salt == null) {
        // 4 bytes
        saltBytes = new BigInteger((4 * 8), random).toString(32).getBytes("UTF-8");
      }
      else if (byte[].class.isInstance(salt)) {
        saltBytes = (byte[]) salt;
      }
      else {
        throw new IllegalArgumentException("salt must be of type: byte[].");
      }

      // check to make sure we can get the algorithm
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA1");
      }
      catch (NoSuchAlgorithmException e) {
        throw new RuntimeException("Digest SHA not supported on this JVM.");
      }

      // digest
      md.update(password.getBytes("UTF-8"));

      md.update(saltBytes);
      byte[] digested = md.digest();

      // toBeEncoded = digest + salt
      byte[] toBeEncoded = new byte[digested.length + saltBytes.length];
      System.arraycopy(digested, 0, toBeEncoded, 0, digested.length);
      System.arraycopy(saltBytes, 0, toBeEncoded, digested.length, saltBytes.length);

      return "{SSHA}" + Base64.encodeToString(toBeEncoded);

    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("This JVM failed to get bytes in UTF-8 from String: " + salt, e);
    }
  }

  @Override
  public boolean isPasswordValid(String encPassword, String inputPassword, Object salt) {
    // check for null
    if (inputPassword == null) {
      return false;
    }

    String encryptedPassword = encPassword;
    // strip off the prefix
    if (encryptedPassword.startsWith(SSHA_PREFIX) || encryptedPassword.startsWith(SSHA_PREFIX.toLowerCase())) {
      encryptedPassword = encryptedPassword.substring(SSHA_PREFIX.length());
    }

    try {
      byte[] decodedBytes = Base64.decode(encryptedPassword.getBytes("UTF-8"));

      // strip the first 20 char, but make sure it is valie
      if (decodedBytes.length - 20 <= 0) {
        return false;
      }

      byte[] decryptSalt = new byte[decodedBytes.length - 20];
      System.arraycopy(decodedBytes, 20, decryptSalt, 0, decryptSalt.length);

      String check = this.encodePassword(inputPassword, decryptSalt);
      return check.substring(SSHA_PREFIX.length()).equals(encryptedPassword);

    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("This JVM failed to get bytes in UTF-8 from String: " + salt, e);
    }
  }
}
