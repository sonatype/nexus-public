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

import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.proxy.ProxyFacet;

/**
 * Helper for group rendered view operations.
 * <p>
 * Provides TTL-overlay logic lifted from Maven's
 * {@code MavenContentGroupFacetImpl.computeMinProxyMetadataMaxAgeSeconds}.
 *
 * @see GroupRenderedViewFacet
 */
public final class GroupRenderedViewSupport
{
  /**
   * Returned when no TTL bound can be derived, meaning callers must not apply a time-based
   * staleness check. Callers are expected to guard on {@code >= 0} before using the value.
   */
  public static final int NO_TTL_BOUND = -1;

  /**
   * TTL applied to a group with <strong>no proxy members at all</strong> (typically a
   * hosted-only group). Such a group has no proxy {@code metadataMaxAge} to derive a bound
   * from, and without one a materialised rendered view would have no time-based staleness
   * limit whatsoever — an unbounded-staleness hazard, since the rendered view deliberately
   * suppresses member dispatch on a fresh hit.
   * <p>
   * The value matches Nexus's own default proxy {@code metadataMaxAge}
   * ({@code ProxyFacetSupport} defaults to {@link Duration#ofHours(int) 24 hours}), so a
   * hosted-only group is bounded no more loosely than a default-configured proxy would be.
   */
  public static final int NO_PROXY_MEMBER_MAX_AGE_SECONDS = (int) Duration.ofHours(24).toSeconds();

  private GroupRenderedViewSupport() {
    // utility class
  }

  /**
   * Returns the minimum {@code metadataMaxAge} in seconds across all proxy leaf members.
   * <p>
   * Used to enforce proxy TTLs on the group's merged metadata cache, which otherwise has an
   * infinite TTL. Two cases yield no derived minimum, and they are deliberately treated
   * differently:
   * <ul>
   * <li><strong>No proxy members at all</strong> — returns
   * {@link #NO_PROXY_MEMBER_MAX_AGE_SECONDS}, giving hosted-only groups an explicit finite
   * bound instead of unbounded staleness.</li>
   * <li><strong>Proxy members exist but every one reports a negative
   * {@code metadataMaxAge}</strong> — returns {@link #NO_TTL_BOUND}. A negative value is an
   * explicit "never expire" choice on the member, and overriding it here would silently
   * impose a stricter policy than the operator configured.</li>
   * </ul>
   *
   * @param leafMembers the leaf member repositories to examine
   * @return the minimum metadata max age in seconds, {@link #NO_PROXY_MEMBER_MAX_AGE_SECONDS}
   *         when there are no proxy members, or {@link #NO_TTL_BOUND} when all proxy members
   *         report a negative max age
   */
  public static int computeMinProxyMetadataMaxAgeSeconds(final Collection<Repository> leafMembers) {
    return computeMinProxyMetadataMaxAgeSeconds(leafMembers, NO_PROXY_MEMBER_MAX_AGE_SECONDS);
  }

  /**
   * Same as {@link #computeMinProxyMetadataMaxAgeSeconds(Collection)}, but lets the caller supply
   * its own default for the no-proxy-members case instead of {@link #NO_PROXY_MEMBER_MAX_AGE_SECONDS}.
   * <p>
   * Formats with their own default proxy {@code metadataMaxAge} (e.g. npm) should pass that value
   * here so a hosted-only group is bounded consistently with the format's own defaults.
   *
   * @param leafMembers the leaf member repositories to examine
   * @param noProxyMemberDefaultSeconds the value to return when there are no proxy members
   * @return the minimum metadata max age in seconds, {@code noProxyMemberDefaultSeconds} when
   *         there are no proxy members, or {@link #NO_TTL_BOUND} when all proxy members report a
   *         negative max age
   */
  public static int computeMinProxyMetadataMaxAgeSeconds(
      final Collection<Repository> leafMembers,
      final int noProxyMemberDefaultSeconds)
  {
    List<Integer> proxyMaxAges = leafMembers.stream()
        .flatMap(member -> member.optionalFacet(ProxyFacet.class).stream())
        .map(proxy -> (int) Math.min(
            proxy.getConfiguration().getMetadataMaxAge().getSeconds(), Integer.MAX_VALUE))
        .toList();

    if (proxyMaxAges.isEmpty()) {
      return noProxyMemberDefaultSeconds;
    }

    return proxyMaxAges.stream()
        .mapToInt(Integer::intValue)
        .filter(seconds -> seconds >= 0)
        .min()
        .orElse(NO_TTL_BOUND);
  }
}
