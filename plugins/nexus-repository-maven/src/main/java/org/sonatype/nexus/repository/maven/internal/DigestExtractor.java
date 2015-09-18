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
package org.sonatype.nexus.repository.maven.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.io.LimitedInputStream;
import org.sonatype.nexus.common.text.Strings2;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for extracting digest strings from Maven .sha1/.md5 files that originates from external source, and
 * might be created by vastly different tools out there.
 *
 * @since 3.0
 */
public class DigestExtractor
{
  private static final int MAX_CHARS_NEEDED = 120;

  private DigestExtractor() {
  }

  /**
   * Extract the digest string from a stream, that carries a payload of a Maven sha1/md5 file. Method closes the
   * passed in stream even in case of IO problem.
   *
   * @see {@link #extract(String)}
   */
  @Nullable
  public static String extract(final InputStream stream)
      throws IOException
  {
    checkNotNull(stream);
    try (InputStreamReader isr = new InputStreamReader(
        new LimitedInputStream(stream, 0, MAX_CHARS_NEEDED), Charsets.UTF_8)) {
      return extract(CharStreams.toString(isr));
    }
    finally {
      stream.close();
    }
  }

  /**
   * Extract the checksum from a string. If result appears to be a digest (cannot be 100% sure), it is returned,
   * otherwise {@code null}.
   */
  @Nullable
  public static String extract(final String input) {
    checkNotNull(input);
    // we need at most MAX_CHARS_NEEDED
    String raw = input.length() > MAX_CHARS_NEEDED ? input.substring(0, MAX_CHARS_NEEDED) : input;
    // if multiline, we need the 1st line only
    raw = raw.indexOf('\n') > -1 ? raw.substring(0, raw.indexOf('\n')) : raw;
    // trim whatever we have
    raw = raw.trim();

    // check did we end up with empty string
    if (Strings.isNullOrEmpty(raw)) {
      return null;
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
      digest = compress(raw.substring(0, raw.lastIndexOf(' ')).trim());
    }

    if (isDigest(digest)) {
      return digest;
    }
    else {
      return null;
    }
  }

  /**
   * Returns {@code true} if the input string "looks like" digest hex (md5 or sha1). Method is safe to accept even
   * {@code null}s, naturally that will result with {@code false} result.
   */
  public static boolean isDigest(@Nullable final String digest) {
    if (Strings.isNullOrEmpty(digest)) {
      return false;
    }
    return digest.length() >= 32 && digest.matches("^[a-f0-9]+$");
  }

  /**
   * Removes white space and lower cases the digest string.
   */
  private static String compress(final String digest) {
    return Strings2.lower(digest.replaceAll(" ", ""));
  }
}
