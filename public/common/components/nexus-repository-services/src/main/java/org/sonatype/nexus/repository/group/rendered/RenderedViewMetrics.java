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

public final class RenderedViewMetrics
{
  public static final String PREFIX = "nxrm.group.rendered_view.";

  public static final String HIT = PREFIX + "hit";

  // A hit whose stored URLs had to be rewritten for a request host other than the configured base URL.
  // Also counted as a HIT; a high ratio means the zero-parse fast path is not being taken.
  public static final String HIT_REWRITTEN = PREFIX + "hit_rewritten";

  public static final String MISS = PREFIX + "miss";

  public static final String REBUILD = PREFIX + "rebuild";

  private static final String INVALIDATION_PREFIX = PREFIX + "invalidation_source.";

  public static final String INVALIDATION_ASSET_UPDATED = INVALIDATION_PREFIX + "asset_updated";

  public static final String INVALIDATION_COMPONENT_DELETED = INVALIDATION_PREFIX + "component_deleted";

  public static final String INVALIDATION_FIREWALL_TOGGLE = INVALIDATION_PREFIX + "firewall_toggle";

  public static final String INVALIDATION_MEMBER_ONLINE_TOGGLE = INVALIDATION_PREFIX + "member_online_toggle";

  // Defined for completeness but intentionally not emitted today. A fingerprint mismatch surfaces as a MISS
  // (boolean match) rather than a distinct invalidation reason, so these are unused pending future refinement.
  public static final String INVALIDATION_MEMBER_SET_CHANGED = INVALIDATION_PREFIX + "member_set_changed";

  public static final String INVALIDATION_BASE_URL_CHANGED = INVALIDATION_PREFIX + "base_url_changed";

  public static final String INVALIDATION_TTL_OVERLAY = INVALIDATION_PREFIX + "ttl_overlay";

  private RenderedViewMetrics() {
  }
}
