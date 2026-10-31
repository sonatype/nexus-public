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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Pins the constant values of {@link RenderedViewMetrics}, which moved here from PyPI's
 * format-specific package so npm can share the same counters.
 */
public class RenderedViewMetricsTest
{
  @Test
  public void constantValues_unchangedFromPyPiOrigin() {
    assertThat(RenderedViewMetrics.PREFIX, equalTo("nxrm.group.rendered_view."));
    assertThat(RenderedViewMetrics.HIT, equalTo("nxrm.group.rendered_view.hit"));
    assertThat(RenderedViewMetrics.MISS, equalTo("nxrm.group.rendered_view.miss"));
    assertThat(RenderedViewMetrics.REBUILD, equalTo("nxrm.group.rendered_view.rebuild"));
    assertThat(RenderedViewMetrics.INVALIDATION_ASSET_UPDATED,
        equalTo("nxrm.group.rendered_view.invalidation_source.asset_updated"));
    assertThat(RenderedViewMetrics.INVALIDATION_COMPONENT_DELETED,
        equalTo("nxrm.group.rendered_view.invalidation_source.component_deleted"));
    assertThat(RenderedViewMetrics.INVALIDATION_FIREWALL_TOGGLE,
        equalTo("nxrm.group.rendered_view.invalidation_source.firewall_toggle"));
    assertThat(RenderedViewMetrics.INVALIDATION_MEMBER_ONLINE_TOGGLE,
        equalTo("nxrm.group.rendered_view.invalidation_source.member_online_toggle"));
    assertThat(RenderedViewMetrics.INVALIDATION_MEMBER_SET_CHANGED,
        equalTo("nxrm.group.rendered_view.invalidation_source.member_set_changed"));
    assertThat(RenderedViewMetrics.INVALIDATION_BASE_URL_CHANGED,
        equalTo("nxrm.group.rendered_view.invalidation_source.base_url_changed"));
    assertThat(RenderedViewMetrics.INVALIDATION_TTL_OVERLAY,
        equalTo("nxrm.group.rendered_view.invalidation_source.ttl_overlay"));
  }
}
