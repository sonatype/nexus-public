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
package org.sonatype.security.realms.publickey;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * A {@link PublicKey} wrapper which implements {@code equals}, so they can be compared.
 *
 * @author hugo@josefson.org
 */
class PublicKeyWithEquals
    implements PublicKey
{

  private static final long serialVersionUID = 3668007428213640544L;

  private final PublicKey key;

  /**
   * Constructs this wrapper with the specified key.
   *
   * @param key the {@link PublicKey} to wrap.
   */
  public PublicKeyWithEquals(PublicKey key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PublicKeyWithEquals)) {
      return false;
    }

    PublicKeyWithEquals that = (PublicKeyWithEquals) o;

    final String algorithm = getAlgorithm();
    final String thatAlgorithm = that.getAlgorithm();
    if (algorithm != null ? !algorithm.equals(thatAlgorithm) : thatAlgorithm != null) {
      return false;
    }

    if (!Arrays.equals(getEncoded(), that.getEncoded())) {
      return false;
    }

    final String format = getFormat();
    final String thatFormat = that.getFormat();
    if (format != null ? !format.equals(thatFormat) : thatFormat != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    final String algorithm = getAlgorithm();
    final String format = getFormat();
    final byte[] encoded = getEncoded();
    int result = algorithm != null ? algorithm.hashCode() : 0;
    result = 31 * result + (format != null ? format.hashCode() : 0);
    result = 31 * result + (encoded != null ? Arrays.hashCode(encoded) : 0);
    return result;
  }

  public String getAlgorithm() {
    return key.getAlgorithm();
  }

  public String getFormat() {
    return key.getFormat();
  }

  public byte[] getEncoded() {
    return key.getEncoded();
  }
}
