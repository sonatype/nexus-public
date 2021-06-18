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
package org.sonatype.nexus.repository.r.internal.util;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.r.internal.RException;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Utility methods for working with R metadata in general.
 *
 * @since 3.28
 */
public final class RMetadataUtils
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1);

  /**
   * Parses metadata stored in a Debian Control File-like format.
   *
   * @see <a href="https://cran.r-project.org/doc/manuals/r-release/R-exts.html#The-DESCRIPTION-file">Description File</a>
   */
  public static Map<String, String> parseDescriptionFile(final InputStream in) {
    checkNotNull(in);
    try {
      LinkedHashMap<String, String> results = new LinkedHashMap<>();
      InternetHeaders headers = new InternetHeaders(in);
      Enumeration headerEnumeration = headers.getAllHeaders();
      while (headerEnumeration.hasMoreElements()) {
        Header header = (Header) headerEnumeration.nextElement();
        String name = header.getName();
        String value = header.getValue()
            .replace("\r\n", "\n")
            .replace("\r", "\n"); // TODO: "should" be ASCII only, otherwise need to know encoding?
        results.put(name, value); // TODO: Supposedly no duplicates, is this true?
      }
      return results;
    } catch (MessagingException e) {
      throw new RException(null, e);
    }
  }

  private RMetadataUtils() {
    // empty
  }
}
