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
package org.sonatype.nexus.repository.httpclient.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link ConfigurableAutoBlockConfiguration}.
 */
class ConfigurableAutoBlockConfigurationTest
{
  @Test
  void shouldBlockOnlyListedStatusCodes() {
    ConfigurableAutoBlockConfiguration underTest = new ConfigurableAutoBlockConfiguration(List.of(404, 429));

    assertThat(underTest.shouldBlock(404), is(true));
    assertThat(underTest.shouldBlock(429), is(true));
    assertThat(underTest.shouldBlock(401), is(false));
    assertThat(underTest.shouldBlock(500), is(false));
  }

  @Test
  void shouldNotBlockAnythingWhenListIsEmpty() {
    ConfigurableAutoBlockConfiguration underTest = new ConfigurableAutoBlockConfiguration(List.of());

    assertThat(underTest.shouldBlock(404), is(false));
    assertThat(underTest.shouldBlock(500), is(false));
  }
}
