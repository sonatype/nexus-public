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
package org.sonatype.nexus.ruby;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple convenient wrapper around SHA1 MessageDigest
 */
public class Sha1Digest {

  private final MessageDigest digest;

  /**
   * new Sha1Digest
   */
  public Sha1Digest() {
    try {
      digest = MessageDigest.getInstance("SHA1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("no SHA1 digest on system");
    }
  }

  /**
   * update the underlying digest
   * @param byte to be updated
   */
  public void update(byte b) {
    digest.update(b);
  }

  /**
   * update the underlying digest
   * @param byte[] to be updated
   */
  public void update(byte[] bytes) {
    digest.update(bytes);
  }

  /**
   * finalized the digest and produces a hex-dump
   * @return hex dump of the digest
   */
  public String hexDigest() {
    StringBuilder dig = new StringBuilder();
    for (byte b : digest.digest()) {
      if (b < 0) {
        dig.append(Integer.toHexString(256 + b));
      } else if (b < 16) {
        dig.append("0").append(Integer.toHexString(b));
      } else {
        dig.append(Integer.toHexString(b));
      }
    }
    return dig.toString();
  }
}
