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
package org.sonatype.nexus.audit.protect;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ProtectConfigChangedEventTest
{
  @Test
  public void testEventProperties() {
    ProtectConfigChangedEvent event = new ProtectConfigChangedEvent(
        "protect.firewall", "protection-level-changed",
        "maven-proxy", "none", "quarantine");

    assertThat(event.getDomain(), is("protect.firewall"));
    assertThat(event.getType(), is("protection-level-changed"));
    assertThat(event.getContext(), is("maven-proxy"));
    assertThat(event.getFromValue(), is("none"));
    assertThat(event.getToValue(), is("quarantine"));
  }
}
