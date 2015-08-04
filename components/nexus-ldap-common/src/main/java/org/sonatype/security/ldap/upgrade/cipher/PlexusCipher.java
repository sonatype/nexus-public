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
package org.sonatype.security.ldap.upgrade.cipher;

/**
 * @author Oleg Gusakov
 */
public interface PlexusCipher
{
  public static final char ENCRYPTED_STRING_DECORATION_START = '{';

  public static final char ENCRYPTED_STRING_DECORATION_STOP = '}';

  /**
   * encrypt given string with the given passPhrase and encode it into base64
   */
  String encrypt(
      String str,
      String passPhrase)
      throws PlexusCipherException;

  /**
   * encrypt given string with the given passPhrase, encode it into base64 and
   * return result, wrapped into { } decorations
   */
  String encryptAndDecorate(
      String str,
      String passPhrase)
      throws PlexusCipherException;

  /**
   * decrypt given base64 encrypted string
   */
  String decrypt(
      String str,
      String passPhrase)
      throws PlexusCipherException;

  /**
   * decrypt given base64 encoded encrypted string. If string is decorated,
   * decrypt base64 encoded string inside decorations
   */
  String decryptDecorated(
      String str,
      String passPhrase)
      throws PlexusCipherException;

  /**
   * check if given string is decorated
   */
  public boolean isEncryptedString(
      String str);

  /**
   * return string inside decorations
   */
  public String unDecorate(
      String str)
      throws PlexusCipherException;

  /**
   * decorated given string with { and }
   */
  public String decorate(
      String str);

}
