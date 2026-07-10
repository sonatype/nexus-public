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
package org.sonatype.nexus.httpclient.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for normalizing URI paths to prevent path traversal attacks
 * while preserving percent-encoding for signed URLs and CDN compatibility.
 *
 * This class provides two normalization strategies:
 * <ul>
 * <li>Decoded path normalization - for detecting path traversal sequences</li>
 * <li>Raw path normalization - for removing traversal while preserving encoding</li>
 * </ul>
 */
public final class SecurePathNormalizer
{
  private static final Logger log = LoggerFactory.getLogger(SecurePathNormalizer.class);

  private SecurePathNormalizer() {
    // Utility class
  }

  /**
   * Normalizes a path by removing .. and . segments.
   *
   * This method works for both decoded and raw (percent-encoded) paths because it only
   * looks for literal ".." and "." segments, which are never percent-encoded in practice.
   *
   * Use cases:
   * - Security validation: detect path traversal attempts in decoded paths
   * - Signed URLs: normalize raw paths while preserving percent-encoding (critical for AWS S3,
   * Cloudflare R2, etc. where changing encoding breaks signature validation)
   *
   * Unlike URI.normalize(), this method prevents traversal beyond the root.
   *
   * Examples:
   * - "/path/to/../file" → "/path/file"
   * - "/path/Q1Hny%2BsM/../file" → "/path/Q1Hny%2BsM/file" (preserves %2B encoding)
   *
   * @param path the path to normalize (decoded or raw/percent-encoded)
   * @return normalized path with .. and . segments removed
   */
  public static String normalizePath(final String path) {
    if (path == null || path.isEmpty()) {
      return path;
    }

    // Split path into segments
    String[] segments = path.split("/");
    Deque<String> stack = new ArrayDeque<>();

    for (String segment : segments) {
      // Skip empty segments and current directory references
      if (segment.isEmpty() || ".".equals(segment)) {
        continue;
      }

      if ("..".equals(segment)) {
        // Go up one level, but never beyond root
        if (!stack.isEmpty()) {
          stack.removeLast();
        }
        else {
          // Stack is empty - we're at root. Dropping ".." prevents traversal beyond root.
          // This is secure: we NEVER pop from an empty stack, so we CANNOT go outside root.
          // Log warning to detect potential security probes or malformed paths.
          log.warn("Attempted path traversal beyond root detected, path: {}", path);
        }
      }
      else {
        // Normal segment, add to stack
        stack.addLast(segment);
      }
    }

    return reconstructPath(path, stack);
  }

  /**
   * Checks if a decoded path contains path traversal sequences (.. or .).
   *
   * Only literal "." and ".." segments count as traversal. Empty segments produced by
   * consecutive slashes (e.g. "/a//b") are not path traversal and must not be flagged,
   * because signed URLs (AWS S3, Cloudflare R2) sign the exact path bytes and
   * collapsing "//" to "/" invalidates the signature (NEXUS-52769).
   *
   * @param path the path to check
   * @return true if path traversal is detected
   */
  public static boolean containsPathTraversal(final String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }

    for (String segment : path.split("/")) {
      if (".".equals(segment) || "..".equals(segment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Encodes special characters in a raw path while preserving existing percent-encoding.
   * This is critical for signed URLs where literal special characters (like +, #, ?)
   * must be encoded but already-encoded sequences must remain unchanged.
   *
   * <p>
   * Characters that need encoding:
   * <ul>
   * <li>+ (plus) -> %2B</li>
   * <li># (hash) -> %23</li>
   * <li>? (question mark) -> %3F (only in path, not in query string)</li>
   * <li>Space -> %20</li>
   * </ul>
   *
   * <p>
   * Already-encoded sequences like %2B are preserved as-is.
   *
   * @param rawPath the raw path that may contain literal special characters
   * @return path with special characters encoded, existing encoding preserved
   */
  public static String encodeSpecialCharacters(final String rawPath) {
    if (rawPath == null || rawPath.isEmpty()) {
      return rawPath;
    }

    StringBuilder result = new StringBuilder();
    int i = 0;
    while (i < rawPath.length()) {
      char c = rawPath.charAt(i);

      // Check if this is an already-encoded sequence (%XX)
      if (c == '%' && i + 2 < rawPath.length() &&
          isHexDigit(rawPath.charAt(i + 1)) &&
          isHexDigit(rawPath.charAt(i + 2))) {
        // Preserve existing percent-encoding
        result.append(c);
        result.append(rawPath.charAt(i + 1));
        result.append(rawPath.charAt(i + 2));
        i += 3;
      }
      else {
        // Encode special characters
        switch (c) {
          case '+':
            result.append("%2B");
            break;
          case '#':
            result.append("%23");
            break;
          case ' ':
            result.append("%20");
            break;
          default:
            // Keep all other characters as-is (including /, -, _, ., ~, etc.)
            result.append(c);
            break;
        }
        i++;
      }
    }

    return result.toString();
  }

  /**
   * Check if a character is a hexadecimal digit.
   */
  private static boolean isHexDigit(final char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  /**
   * Reconstructs a path from segments, preserving original path characteristics
   * (leading/trailing slashes).
   *
   * @param originalPath the original path (for reference)
   * @param segments the normalized segments
   * @return reconstructed path string
   */
  private static String reconstructPath(final String originalPath, final Deque<String> segments) {
    StringBuilder result = new StringBuilder();

    // Preserve leading slash if original had one
    if (originalPath.startsWith("/")) {
      result.append("/");
    }

    // Build normalized path from segments
    Iterator<String> iter = segments.iterator();
    while (iter.hasNext()) {
      result.append(iter.next());
      if (iter.hasNext()) {
        result.append("/");
      }
    }

    // Preserve trailing slash if original had one (and we have segments)
    if (originalPath.endsWith("/") && !segments.isEmpty()) {
      result.append("/");
    }

    // Handle special case: if path was just "/" or became empty, return "/"
    if (result.isEmpty() && originalPath.startsWith("/")) {
      return "/";
    }

    return result.toString();
  }
}
