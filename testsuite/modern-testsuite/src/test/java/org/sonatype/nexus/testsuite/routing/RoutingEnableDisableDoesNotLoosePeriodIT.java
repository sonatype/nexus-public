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
package org.sonatype.nexus.testsuite.routing;

import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;
import org.sonatype.sisu.litmus.testsupport.group.Smoke;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * NEXUS-5561: When enabling/disabling remote discovery, the update interval should not be lost.
 *
 * @author cstamas
 */
@Category(Smoke.class)
public class RoutingEnableDisableDoesNotLoosePeriodIT
    extends RoutingITSupport
{

  public RoutingEnableDisableDoesNotLoosePeriodIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Before
  public void waitForDiscoveryOutcome()
      throws Exception
  {
    routingTest().waitForAllRoutingUpdateJobToStop();
    // waitForWLDiscoveryOutcome( "central" );
  }

  @Test
  public void checkRoutingEnableDisableDoesNotLoosePeriod() {
    final int updatePeriod = 11;
    // get configuration for central and check for sane values (actually, they should be defaults).
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(true));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(24));
      // checked ok, set interval to 11, and enable it
      centralConfiguration.setIntervalHours(updatePeriod);
      centralConfiguration.setEnabled(true);
      routing().setDiscoveryConfigurationFor("central", centralConfiguration);
    }
    // verify is set, but feature remains disabled
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(true));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(updatePeriod));
      // disable it
      centralConfiguration.setEnabled(false);
      routing().setDiscoveryConfigurationFor("central", centralConfiguration);
    }
    // verify is set, but feature remains disabled
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(false));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(updatePeriod));
      // enable it
      centralConfiguration.setEnabled(true);
      routing().setDiscoveryConfigurationFor("central", centralConfiguration);
    }
    // verify is set, but feature remains disabled
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(true));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(updatePeriod));
    }
  }

}
