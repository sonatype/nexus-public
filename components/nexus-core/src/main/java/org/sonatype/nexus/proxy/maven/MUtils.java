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
package org.sonatype.nexus.proxy.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import org.sonatype.nexus.proxy.item.StorageFileItem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.codehaus.plexus.util.StringUtils;

/**
 * Maven specific "static" utilities.
 *
 * @author cstamas
 */
public class MUtils
{

  /**
   * This method will read up an entire stream usually created from .sha1/.md5.
   * <p/>
   * The method CLOSES the passed in stream!
   * <b>Important:</b> it is entirely possible this method will return an invalid digest in certain cases, therefore
   * it
   * is up to the caller to ensure the resulting value is in the format they expect.
   *
   * @param inputStream the stream to read the digest from
   * @return never null but the first recognized digest pattern or empty string if inputstream is zero bytes or if the
   *         stream does
   *         not contain a recognized digest pattern, a fallback value that includes the start of the read stream to
   *         the first
   *         space or end of line marker
   * @throws IOException if there was a problem reading the inputStream as UTF-8 encoded text
   * @see #isDigest(String)
   */
  public static String readDigestFromStream(final InputStream inputStream)
      throws IOException
  {
    try (InputStreamReader isr = new InputStreamReader(inputStream, Charsets.UTF_8)) {
      return readDigest(CharStreams.toString(isr));
    }
  }

  @VisibleForTesting
  static String readDigest(final String input) {
    String raw = StringUtils.chomp(input).trim();

    if (StringUtils.isEmpty(raw)) {
      return "";
    }

    String digest;
    // digest string at end with separator, e.g.:
    // MD5 (pom.xml) = 68da13206e9dcce2db9ec45a9f7acd52
    // ant-1.5.jar: DCAB 88FC 2A04 3C24 79A6 DE67 6A2F 8179 E9EA 2167
    if (raw.contains("=") || raw.contains(":")) {
      digest = raw.split("[=:]", 2)[1].trim();
    }
    else {
      // digest string at start, e.g. '68da13206e9dcce2db9ec45a9f7acd52 pom.xml'
      digest = raw.split(" ", 2)[0];
    }

    if (!isDigest(digest)) {
      // maybe it's "uncompressed", e.g. 'DCAB 88FC 2A04 3C24 79A6 DE67 6A2F 8179 E9EA 2167'
      digest = compress(digest);
    }

    if (!isDigest(digest)) {
      // check if the raw string is an uncompressed checksum, e.g.
      // 'DCAB 88FC 2A04 3C24 79A6 DE67 6A2F 8179 E9EA 2167'
      digest = compress(raw);
    }

    if (!isDigest(digest) && raw.contains(" ")) {
      // check if the raw string is an uncompressed checksum with file name suffix, e.g.
      // 'DCAB 88FC 2A04 3C24 79A6 DE67 6A2F 8179 E9EA 2167 pom.xml'
      digest = compress(raw.substring(0, raw.lastIndexOf(" ")).trim());
    }

    if (!isDigest(digest)) {
      // we have to return some string even if it's not a valid digest, because 'null' is treated as
      // "checksum does not exist" elsewhere (AbstractChecksumContentValidator)
      // -> fallback to original behavior
      digest = raw.split(" ", 2)[0];
    }

    return digest;
  }

  private static String compress(String digest) {
    digest = digest.replaceAll(" ", "").toLowerCase(Locale.US);
    return digest;
  }

  /**
   * Validates a string meets the minimum requirements of being a potential digest value, namely a length greater
   * than
   * or equal to 32 characters and the entire string matches the regex {@code ^[a-z0-9]+$}.
   *
   * @param digest the text to validate as a digest
   * @return true if digest passes validation
   */
  public static boolean isDigest(String digest) {
    return digest.length() >= 32 && digest.matches("^[a-z0-9]+$");
  }

  /**
   * Reads up a hash as string from StorageFileItem pointing to .sha1/.md5 files.
   */
  public static String readDigestFromFileItem(final StorageFileItem inputFileItem)
      throws IOException
  {
    return readDigestFromStream(inputFileItem.getInputStream());
  }
}
