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
package org.sonatype.nexus.repository.pypi.internal;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.hash.HashCode;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiPathUtils.normalizeName;

/**
 * @since 3.next
 */
public class PyPiStorageUtils
{
  private final static String MD5_ATTRIBUTE_NAME = "md5_digest";

  public static void validateMd5Hash(final Map<String, String> attributes, final TempBlob tempBlob) {
    if (attributes.containsKey(MD5_ATTRIBUTE_NAME)) {
      String expectedDigest = attributes.get(MD5_ATTRIBUTE_NAME);
      HashCode hashCode = tempBlob.getHashes().get(MD5);
      String hashValue = hashCode.toString();
      if (!expectedDigest.equalsIgnoreCase(hashValue)) {
        throw new IllegalOperationException(
            "Digests do not match, found: " + hashValue + ", expected: " + expectedDigest);
      }
    }
  }

  /**
   * We supply all variants of the name. According to pep-503 '-', '.' and '_' are to be treated equally.
   * This allows searching for this component to be found using any combination of the substituted characters.
   * When using only this technique and not parsing the actual metadata stored in the package, the original name
   * (if it contained multiple special characters) would not be accounted for in search.
   *
   * @param name       of the component to be saved
   */
  public static Map<String, String> getNameAttributes(final String name) {
    Map<String, String> nameAttributes = new HashMap<>();
    nameAttributes.put(P_NAME, name);
    nameAttributes.put("name_dash", normalizeName(name, "-"));
    nameAttributes.put("name_dot", normalizeName(name, "."));
    nameAttributes.put("name_underscore", normalizeName(name, "_"));
    return nameAttributes;
  }
}
