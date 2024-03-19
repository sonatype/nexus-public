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
package org.sonatype.nexus.onboarding.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class InstanceStatusTest
    extends TestSupport
{
  private InstanceStatus underTest;

  @Before
  public void setup() {

    underTest = new InstanceStatus();
  }

  @Test
  public void testIsNew() {

    assertThat(underTest.isNew(), is(true));
    //assertThat(underTest.isUpgraded(), is(false));
  }

  @Test
  public void testIsUpgraded() {

    //assertThat(underTest.isNew(), is(false));
    assertThat(underTest.isUpgraded(), is(true));
  }
}
