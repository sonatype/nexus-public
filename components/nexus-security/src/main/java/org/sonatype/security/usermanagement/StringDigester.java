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
package org.sonatype.security.usermanagement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

/**
 * A util class to calculate various digests on Strings. Usaful for some simple password management.
 *
 * @author cstamas
 */
public class StringDigester
{

  /**
   * Calculates a digest for a String user the requested algorithm.
   */
  public static String getDigest(String alg, String content)
      throws NoSuchAlgorithmException
  {
    String result = null;

    try {
      InputStream fis = new ByteArrayInputStream(content.getBytes("UTF-8"));

      try {
        byte[] buffer = new byte[1024];

        MessageDigest md = MessageDigest.getInstance(alg);

        int numRead;

        do {
          numRead = fis.read(buffer);
          if (numRead > 0) {
            md.update(buffer, 0, numRead);
          }
        }
        while (numRead != -1);

        result = new String(Hex.encodeHex(md.digest()));
      }
      finally {
        fis.close();
      }
    }
    catch (IOException e) {
      // hrm
      result = null;
    }

    return result;
  }

  /**
   * Calculates a SHA1 digest for a string.
   */
  public static String getSha1Digest(String content) {
    try {
      return getDigest("SHA1", content);
    }
    catch (NoSuchAlgorithmException e) {
      // will not happen
      return null;
    }
  }

}
