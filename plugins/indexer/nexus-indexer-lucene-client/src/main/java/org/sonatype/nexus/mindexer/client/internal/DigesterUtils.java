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
package org.sonatype.nexus.mindexer.client.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class to calculate various digests on Strings. Useful for some simple content verification, checksumming.
 *
 * @author cstamas
 */
public class DigesterUtils
{
  public static final String SHA1_ALG = "SHA1";

  public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

  /**
   * Hex Encodes the digest value.
   */
  public static String getDigestAsString(final byte[] digest) {
    return new String(encodeHex(digest));
  }

  /**
   * Calculates a digest for a String user the requested algorithm.
   */
  public static byte[] getDigest(final String alg, final InputStream is)
      throws NoSuchAlgorithmException, IOException
  {
    byte[] result = null;
    try {
      final byte[] buffer = new byte[1024];
      final MessageDigest md = MessageDigest.getInstance(alg);
      int numRead;
      do {
        numRead = is.read(buffer);
        if (numRead > 0) {
          md.update(buffer, 0, numRead);
        }
      }
      while (numRead != -1);
      result = md.digest();
    }
    finally {
      close(is);
    }
    return result;
  }

  // SHA1

  public static byte[] getSha1Digest(final byte[] byteArray) {
    try {
      return getDigest(SHA1_ALG, new ByteArrayInputStream(byteArray));
    }
    catch (NoSuchAlgorithmException e) {
      // will not happen
      return null;
    }
    catch (IOException e) {
      // will not happen
      return null;
    }
  }

  public static String getSha1DigestAsString(final byte[] byteArray) {
    return getDigestAsString(getSha1Digest(byteArray));
  }

  /**
   * Calculates a SHA1 digest for a string.
   */
  public static byte[] getSha1Digest(final String content) {
    try {
      return getDigest(SHA1_ALG, new ByteArrayInputStream(content.getBytes(UTF8_CHARSET)));
    }
    catch (NoSuchAlgorithmException e) {
      // will not happen
      return null;
    }
    catch (IOException e) {
      // will not happen
      return null;
    }
  }

  public static String getSha1DigestAsString(final String content) {
    return getDigestAsString(getSha1Digest(content));
  }

  /**
   * Calculates a SHA1 digest for a stream.
   */
  public static byte[] getSha1Digest(final InputStream is)
      throws IOException
  {
    try {
      return getDigest(SHA1_ALG, is);
    }
    catch (NoSuchAlgorithmException e) {
      // will not happen
      return null;
    }
  }

  public static String getSha1DigestAsString(final InputStream is)
      throws IOException
  {
    return getDigestAsString(getSha1Digest(is));
  }

  /**
   * Calculates a SHA1 digest for a file.
   */
  public static byte[] getSha1Digest(final File file)
      throws IOException
  {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      return getDigest(SHA1_ALG, fis);
    }
    catch (NoSuchAlgorithmException e) {
      return null;
    }
    finally {
      close(fis);
    }
  }

  public static String getSha1DigestAsString(final File file)
      throws IOException
  {
    return getDigestAsString(getSha1Digest(file));
  }

  // -- IOUtil

  public static void close(final InputStream inputStream) {
    if (inputStream == null) {
      return;
    }
    try {
      inputStream.close();
    }
    catch (IOException ex) {
      // ignore
    }
  }

  // -- Hex

  private static final char[] DIGITS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
      'f'
  };

  /**
   * Blatantly copied from commons-codec version 1.3
   */
  public static char[] encodeHex(final byte[] data) {
    int l = data.length;
    char[] out = new char[l << 1];
    // two characters form the hex value.
    for (int i = 0, j = 0; i < l; i++) {
      out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
      out[j++] = DIGITS[0x0F & data[i]];
    }
    return out;
  }
}
