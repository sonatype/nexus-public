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
package org.sonatype.nexus.repository.http;

import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Strings;
import com.google.common.collect.Range;

import static java.util.Collections.singletonList;

/**
 * Parses the "Range" request header.
 *
 * Defined by <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35">RFC 2616 14.35</a>
 *
 * @since 3.0
 */
class RangeParser
    extends ComponentSupport
{
  public static final List<Range<Long>> UNSATISFIABLE = null;

  public static final List<Range<Long>> WHOLE_RANGE = Collections.emptyList();

  /**
   * Returns a list of {@link Range}s, each indicating a range of byte indices (inclusive).
   *
   * Range: bytes=0-10 (from byte 0 to byte 10)
   * Range: bytes=500-999 (from byte 500 to byte 999)
   * Range: bytes=500- (from byte 500 to the end)
   * Range: bytes=-500 (the last 500 bytes, per the RFC)
   *
   * @return {@code null} if the requested range cannot be satisfied given the size of the content, or an empty list in
   * the case of parsing errors
   */
  public List<Range<Long>> parseRangeSpec(final String rangeHeader, long size) {
    Range<Long> content = Range.closed(0L, size - 1L);

    // TODO: Current limitation: only one Range of bytes supported in forms of "-X", "X-Y" (where X<Y) and "X-".
    if (!Strings.isNullOrEmpty(rangeHeader)) {
      try {
        if (rangeHeader.startsWith("bytes=") && rangeHeader.length() > 6 && !rangeHeader.contains(",")) {
          final String rangeSpec = rangeHeader.substring(6, rangeHeader.length());
          if (rangeSpec.startsWith("-")) {
            final long byteCount = Long.parseLong(rangeSpec.substring(1));
            if (byteCount > size) {
              return UNSATISFIABLE;
            }
            final Range<Long> suffix = Range.atLeast(size - byteCount);
            return ensureSatisfiable(suffix, content);
          }
          else if (rangeSpec.endsWith("-")) {
            final Range<Long> requested = Range.atLeast(Long.parseLong(rangeSpec.substring(0, rangeSpec.length() - 1)));
            return ensureSatisfiable(requested, content);
          }
          else if (rangeSpec.contains("-")) {
            final String[] parts = rangeSpec.split("-");
            return ensureSatisfiable(Range.closed(Long.parseLong(parts[0]), Long.parseLong(parts[1])), content);
          }
          else {
            log.warn("Malformed HTTP Range value: {}, ignoring it", rangeHeader);
          }
        }
        else {
          log.warn("Unsupported non-byte or multiple HTTP Ranges: {}; sending complete content", rangeHeader);
        }
      }
      catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.debug("Problem parsing Range value: {}, ignoring", rangeHeader, e);
        }
        else {
          log.warn("Problem parsing Range value: {}, ignoring: {}", rangeHeader, e.toString());
        }
      }
    }

    return WHOLE_RANGE;
  }

  private List<Range<Long>> ensureSatisfiable(Range<Long> requested, Range<Long> content) {
    if (requested.isConnected(content)) {
      return singletonList(requested.intersection(content));
    }
    else {
      return UNSATISFIABLE;
    }
  }

  private boolean isSatisfiable(final Range<Long> range, final long contentSize) {
    if (!range.hasLowerBound()) {
      return true;
    }
    // Per RFC 2616, a requested range is satisfiable as long as its lower bound is within the content size.
    // Requests for ranges that extend beyond the content size are okay.
    return range.lowerEndpoint() < contentSize - 1;
  }
}
