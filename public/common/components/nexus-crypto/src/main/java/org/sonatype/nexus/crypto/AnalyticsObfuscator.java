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
package org.sonatype.nexus.crypto;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.springframework.stereotype.Component;

/**
 * Computes a stable lowercase-hex SHA-1 digest of a UTF-8 string, used as a cross-event join key in
 * the analytics pipeline.
 *
 * <p>
 * This is <b>not</b> a privacy control. Telemetry events may legitimately emit the plaintext
 * identifier alongside its digest — the digest exists so the downstream warehouse can join
 * analytics events emitted by different systems (NXRM
 * {@code HOSTED_REPOSITORY_CONFIGURATION.repository_name} against IQ
 * {@code APPLICATION_EVALUATION_COMPONENT_COUNTS.repository_name}) on a value that both sides can
 * compute deterministically. Whether plaintext is also emitted is a per-event decision governed by
 * the event's schema, not by this class.
 *
 * <p>
 * Uses BouncyCastle's {@link SHA1Digest} rather than
 * {@code MessageDigest.getInstance("SHA-1")} so that installations running with a FIPS-restricted
 * JCE provider (which may reject SHA-1 for general-purpose hashing) still produce this digest.
 * Output is byte-for-byte identical to Sonatype IQ Server's
 * {@code com.sonatype.insight.scan.util.HashUtils.hash(source, HashUtils.SHA1)}, including for null
 * input (which is treated as an empty string, matching IQ).
 */
@Component
public class AnalyticsObfuscator
{
  private static final char[] HEX_LOWERCASE = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  public String obfuscate(final String value) {
    byte[] input = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    SHA1Digest digest = new SHA1Digest();
    digest.update(input, 0, input.length);
    byte[] output = new byte[digest.getDigestSize()];
    digest.doFinal(output, 0);
    return toHex(output);
  }

  private static String toHex(final byte[] digest) {
    char[] chars = new char[digest.length * 2];
    for (int i = 0; i < digest.length; i++) {
      int hi = (digest[i] & 0xF0) >>> 4;
      int lo = digest[i] & 0x0F;
      chars[(i << 1)] = HEX_LOWERCASE[hi];
      chars[(i << 1) + 1] = HEX_LOWERCASE[lo];
    }
    return new String(chars);
  }
}
