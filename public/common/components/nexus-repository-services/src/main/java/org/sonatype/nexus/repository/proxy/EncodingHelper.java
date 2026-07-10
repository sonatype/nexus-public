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
package org.sonatype.nexus.repository.proxy;

import org.sonatype.nexus.common.template.EscapeHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class for encoding URLs based on configured encoding mode.
 *
 * Applies base URL encoding before format-specific encoding rules. This is the first stage
 * of the two-stage encoding pipeline used by ProxyFacetSupport.
 */
public class EncodingHelper
{
  private final EscapeHelper escapeHelper;

  /**
   * Create an EncodingHelper for preserve-encoded-characters mode.
   *
   * The disabled path (preserveEncodedCharacters == false) is represented by a null
   * EncodingHelper in ProxyFacetSupport — this class is only instantiated when the flag is on.
   *
   * @param escapeHelper the escape helper for standard encoding
   */
  public EncodingHelper(final EscapeHelper escapeHelper) {
    this.escapeHelper = checkNotNull(escapeHelper);
  }

  /**
   * Encode URL segments, preserving special characters like %2B, %23, %3F.
   *
   * This is the first stage of encoding, before format-specific rules are applied.
   *
   * @param url the URL to encode
   * @return the encoded URL
   */
  public String encodeUrlSegments(final String url) {
    checkNotNull(url);
    return encodeSpecialChars(url);
  }

  /**
   * Encode special characters explicitly.
   *
   * Used when preserveEncodedCharacters is true to support remotes like AWS S3, Cloudflare, Azure
   * that expect special characters like + to be percent-encoded as %2B.
   *
   * Applies EscapeHelper rules first (encodes %, :, space), then additionally encodes
   * characters that are commonly problematic.
   *
   * RFC 3986 Reserved Characters - Why We Encode Some But Not Others:
   *
   * Characters we DO encode:
   * - % (EscapeHelper) - Must be first to prevent double-encoding
   * - : (EscapeHelper) - Causes issues in path segments for some services
   * - + (EncodingHelper) - Ambiguous (space or literal) - causes signature mismatches in signed URLs
   * - # (EncodingHelper) - Fragment identifier - would break URL parsing
   * - ? (EncodingHelper) - Query separator - would break URL parsing
   * - [, ] (EncodingHelper) - IPv6 delimiters - reserved but can cause issues in paths
   *
   * Characters we DON'T encode (safe in path segments):
   * - / - Path separator - encoding would break path structure (we split on / and encode segments)
   * - @ - Safe in paths, used for credentials in authority section
   * - !$&'()*,;= - Sub-delimiters that are safe in paths and don't cause signed URL issues
   *
   * The goal is selective encoding: encode only characters that break URL parsing or cause
   * signature mismatches with signed URLs, while preserving characters that are safe in paths.
   * This maintains backward compatibility and avoids unnecessary encoding that could cause
   * issues with legacy clients or repositories.
   *
   * @param url the URL to encode
   * @return the encoded URL
   */
  private String encodeSpecialChars(final String url) {
    // Start with EscapeHelper rules (encodes %, :, space)
    String encoded = escapeHelper.uriSegments(url);

    // Explicitly encode additional problematic characters
    encoded = encoded.replace("+", "%2B"); // Plus sign - AWS S3, Cloudflare, etc.
    encoded = encoded.replace("#", "%23"); // Fragment identifier - breaks URLs
    encoded = encoded.replace("?", "%3F"); // Query separator - breaks URLs
    encoded = encoded.replace("[", "%5B"); // Reserved character
    encoded = encoded.replace("]", "%5D"); // Reserved character

    return encoded;
  }

}
