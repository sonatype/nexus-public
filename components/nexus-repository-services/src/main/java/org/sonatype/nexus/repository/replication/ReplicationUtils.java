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
package org.sonatype.nexus.repository.replication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.common.hash.HashAlgorithm;

import org.apache.commons.lang.StringUtils;
import com.google.common.hash.HashCode;

public class ReplicationUtils {

  public static final String CHECKSUM = "checksum";

  private ReplicationUtils() {}

  private static final List<HashAlgorithm> HASH_ALGORITHMS = Arrays.asList(
    HashAlgorithm.MD5,
    HashAlgorithm.SHA256,
    HashAlgorithm.SHA1,
    HashAlgorithm.SHA512);

  public static Map<HashAlgorithm, HashCode> getChecksumsFromProperties(final Map<String, Object> attributesMap) {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();
    for (HashAlgorithm hashAlgorithm : HASH_ALGORITHMS) {
      getChecksumAttribute(attributesMap, hashAlgorithm.name())
        .ifPresent(value -> checksums.put(hashAlgorithm, HashCode.fromString(value)));
    }
    return checksums;
  }

  private static Optional<String> getChecksumAttribute(final Map<String, Object> attributesMap, final String name) {
    try {
      String value = ((Map<String, Object>) attributesMap.get(CHECKSUM)).get(name).toString();
      if (StringUtils.isEmpty(value)) {
        return Optional.empty();
      }
      else {
        return Optional.of(value);
      }
    }
    catch (Exception e) {
      return Optional.empty();
    }
  }
}
