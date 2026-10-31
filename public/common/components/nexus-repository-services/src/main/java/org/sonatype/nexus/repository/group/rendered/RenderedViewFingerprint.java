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
package org.sonatype.nexus.repository.group.rendered;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;

import static java.util.Objects.requireNonNull;

/**
 * Immutable value type capturing the rendering inputs that have no change event of their own.
 * <p>
 * Used to detect whether a cached rendered view is still valid by comparing the member set
 * and base URL against the stored fingerprint token.
 *
 * @see GroupRenderedViewFacet
 */
public final class RenderedViewFingerprint
{
  private static final String NULL_MARKER = "";

  private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

  private final String baseUrl;

  private final String memberSetHash;

  private RenderedViewFingerprint(final String baseUrl, final String memberSetHash) {
    this.baseUrl = baseUrl;
    this.memberSetHash = requireNonNull(memberSetHash);
  }

  /**
   * Creates a fingerprint from the given base URL and ordered member repository names.
   * <p>
   * The member set hash is computed as SHA-256 using length-prefixed encoding of member names
   * to prevent boundary collision attacks. Order matters: a different ordering produces a different hash.
   *
   * @param baseUrl the base URL of the group repository (may be null)
   * @param orderedMemberNames the ordered list of member repository names (must not be null)
   * @return a new fingerprint instance
   */
  public static RenderedViewFingerprint of(@Nullable final String baseUrl, final List<String> orderedMemberNames) {
    return of(baseUrl, orderedMemberNames, Collections.emptyList());
  }

  /**
   * Creates a fingerprint from the given base URL, ordered member repository names, and additional
   * opt-in inputs to fold into the member set hash (e.g. a format-specific PCCS state marker).
   * <p>
   * An empty {@code extraFingerprintInputs} produces a hash identical to {@link #of(String, List)}.
   *
   * @param baseUrl the base URL of the group repository (may be null)
   * @param orderedMemberNames the ordered list of member repository names (must not be null)
   * @param extraFingerprintInputs additional ordered inputs to fold into the hash (must not be null)
   * @return a new fingerprint instance
   */
  public static RenderedViewFingerprint of(
      @Nullable final String baseUrl,
      final List<String> orderedMemberNames,
      final List<String> extraFingerprintInputs)
  {
    requireNonNull(orderedMemberNames);
    requireNonNull(extraFingerprintInputs);
    String memberSetHash = computeMemberSetHash(orderedMemberNames, extraFingerprintInputs);
    return new RenderedViewFingerprint(baseUrl, memberSetHash);
  }

  /**
   * Returns a single stable string suitable for storing on an asset attribute.
   * <p>
   * Format: {@code baseUrl|memberSetHash} where null baseUrl is encoded as empty string.
   *
   * @return the token representation of this fingerprint
   */
  public String toToken() {
    String encodedBaseUrl = baseUrl != null ? baseUrl : NULL_MARKER;
    return encodedBaseUrl + "|" + memberSetHash;
  }

  /**
   * Parses a token created by {@link #toToken()} back into a fingerprint.
   * <p>
   * The <em>last</em> {@code |} is the separator, so a base URL that itself contains {@code |}
   * round-trips correctly. This is unambiguous because the hash suffix is always exactly 64
   * lowercase hex characters, and {@code |} is not a hex character.
   *
   * @param token the token string (must not be null)
   * @return the fingerprint represented by the token
   * @throws IllegalArgumentException if the token is malformed
   */
  public static RenderedViewFingerprint fromToken(final String token) {
    requireNonNull(token);
    int separatorIndex = token.lastIndexOf('|');
    if (separatorIndex < 0) {
      throw new IllegalArgumentException("Malformed fingerprint token: missing separator");
    }
    String baseUrlPart = token.substring(0, separatorIndex);
    String hashPart = token.substring(separatorIndex + 1);
    String baseUrl = NULL_MARKER.equals(baseUrlPart) ? null : baseUrlPart;
    if (hashPart.isEmpty()) {
      throw new IllegalArgumentException("Malformed fingerprint token: empty member set hash");
    }
    if (!SHA256_HEX_PATTERN.matcher(hashPart).matches()) {
      throw new IllegalArgumentException("Malformed fingerprint token: invalid member set hash format");
    }
    return new RenderedViewFingerprint(baseUrl, hashPart);
  }

  /**
   * Returns true iff the stored token equals this fingerprint's token.
   * <p>
   * Null or blank stored token returns false (treated as needs-rebuild).
   *
   * @param storedToken the token to compare against (may be null or blank)
   * @return true if the tokens match, false otherwise
   */
  public boolean matches(@Nullable final String storedToken) {
    if (storedToken == null || storedToken.isBlank()) {
      return false;
    }
    return toToken().equals(storedToken);
  }

  /**
   * Returns the base URL, or null if not set.
   */
  @Nullable
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Returns the SHA-256 hash of the ordered member set.
   */
  public String getMemberSetHash() {
    return memberSetHash;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RenderedViewFingerprint that = (RenderedViewFingerprint) o;
    return Objects.equals(baseUrl, that.baseUrl) && memberSetHash.equals(that.memberSetHash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, memberSetHash);
  }

  @Override
  public String toString() {
    return "RenderedViewFingerprint{" +
        "baseUrl='" + baseUrl + '\'' +
        ", memberSetHash='" + memberSetHash + '\'' +
        '}';
  }

  /**
   * Computes a collision-safe hash of member names using length-prefixed encoding.
   * <p>
   * Each member name is encoded as {@code length + ":" + name} and concatenated.
   * This prevents boundary collision attacks where names containing separator
   * characters could produce the same hash as multiple different names.
   * For example, without this protection, ["a","bc"] and ["ab","c"] would have
   * the same hash when using newline separators.
   *
   * @param memberNames the ordered list of member names
   * @return a SHA-256 hex string
   */
  @VisibleForTesting
  static String computeMemberSetHash(final List<String> memberNames) {
    return computeMemberSetHash(memberNames, Collections.emptyList());
  }

  /**
   * Computes a collision-safe hash of member names plus additional opt-in inputs, using
   * length-prefixed encoding.
   * <p>
   * Each member name and extra input is encoded as {@code length + ":" + value} and concatenated,
   * member names first, then extras, both in order. This prevents boundary collision attacks where
   * values containing separator characters could produce the same hash as multiple different values.
   * An empty {@code extraInputs} produces a hash identical to {@link #computeMemberSetHash(List)}.
   *
   * @param memberNames the ordered list of member names
   * @param extraInputs additional ordered inputs to fold into the hash
   * @return a SHA-256 hex string
   */
  @VisibleForTesting
  static String computeMemberSetHash(final List<String> memberNames, final List<String> extraInputs) {
    StringBuilder encoded = new StringBuilder();
    for (String name : memberNames) {
      encoded.append(name.length()).append(':').append(name);
    }
    for (String extra : extraInputs) {
      encoded.append(extra.length()).append(':').append(extra);
    }
    return Hashing.sha256().hashString(encoded, StandardCharsets.UTF_8).toString();
  }
}
