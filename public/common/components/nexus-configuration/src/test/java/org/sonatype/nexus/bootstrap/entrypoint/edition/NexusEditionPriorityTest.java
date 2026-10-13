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
package org.sonatype.nexus.bootstrap.entrypoint.edition;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionPriority.ENTERPRISE_PRIORITY;
import static org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionPriority.PRO_PLUS_PRIORITY;
import static org.sonatype.nexus.bootstrap.entrypoint.edition.NexusEditionPriority.PRO_PRIORITY;

/**
 * Guards the resolution-order invariant (BDD-057). This is about the priority <b>constants</b>, and
 * is distinct from {@link NexusEditionSelectorTest}, which asserts that <em>registration</em> order
 * does not matter (F14) — true of registration, false of the constants; do not conflate them.
 * <p>
 * A lower priority value is evaluated first by {@link NexusEditionSelector}, so the most specific
 * edition must have the lowest value. The licenses we issue deliberately carry the older editions'
 * features, so an Enterprise/Pro+ license also matches Pro. If a change ever placed Pro+ or
 * Enterprise at or after Pro, such a license would silently resolve to Pro with every Pro
 * capability working and nothing logged — this test fails the build before that can ship.
 */
public class NexusEditionPriorityTest
{
  @Test
  public void proPlusAndEnterpriseResolveAheadOfPro() {
    assertThat(
        "Enterprise must resolve before Pro+, otherwise an Enterprise license silently drops to Pro+",
        ENTERPRISE_PRIORITY, lessThan(PRO_PLUS_PRIORITY));
    assertThat(
        "Pro+ must resolve before Pro, otherwise a Pro+ license silently drops to Pro (BDD-001)",
        PRO_PLUS_PRIORITY, lessThan(PRO_PRIORITY));
    assertThat(
        "Enterprise must resolve before Pro, otherwise an Enterprise license silently drops to Pro (BDD-002)",
        ENTERPRISE_PRIORITY, lessThan(PRO_PRIORITY));
  }
}
