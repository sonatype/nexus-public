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

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;
import org.sonatype.nexus.client.core.subsystem.routing.Status;
import org.sonatype.nexus.client.core.subsystem.routing.Status.Outcome;
import org.sonatype.sisu.goodies.testsupport.group.Smoke;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Simple smoke IT for automatic routing REST being responsive and is reporting the expected statuses when the feature
 * is
 * DISABLED!
 *
 * @author cstamas
 */
@Category(Smoke.class)
public class RoutingDisabledSmokeIT
    extends RoutingITSupport
{

  /**
   * Constructor.
   */
  public RoutingDisabledSmokeIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    // setting the system property to DISABLE feature
    return super.configureNexus(configuration)
        .setSystemProperty(
            "org.sonatype.nexus.proxy.maven.routing.Config.featureActive", Boolean.FALSE.toString()
        );
  }

  @Before
  public void waitForDiscoveryOutcome()
      throws Exception
  {
    routingTest().waitForAllRoutingUpdateJobToStop();
    // waitForWLDiscoveryOutcome( "central" );
  }

  @Test
  public void checkPublicGroupResponse() {
    // public
    final Status publicStatus = routing().getStatus("public");
    assertThat(publicStatus.getPublishedStatus(), equalTo(Outcome.FAILED));
    assertThat(publicStatus.getDiscoveryStatus(), is(nullValue()));
  }

  @Test
  public void checkReleasesHostedResponse() {
    // releases
    final Status releasesStatus = routing().getStatus("releases");
    assertThat(releasesStatus.getPublishedStatus(), equalTo(Outcome.FAILED));
    assertThat(releasesStatus.getDiscoveryStatus(), is(nullValue()));
  }

  @Test
  public void checkCentralProxyResponse() {
    // central
    final Status centralStatus = routing().getStatus("central");
    assertThat(centralStatus.getPublishedStatus(), equalTo(Outcome.FAILED));
    assertThat(centralStatus.getDiscoveryStatus(), is(notNullValue()));
    assertThat(centralStatus.getDiscoveryStatus().getDiscoveryLastStatus(), equalTo(Outcome.UNDECIDED));
  }

  @Test
  public void checkCentralProxyConfiguration() {
    // get configuration for central and check for sane values (actually, they should be defaults).
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(false));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(24));
      // checked ok, set interval to 12h, and enable it
      centralConfiguration.setIntervalHours(12);
      centralConfiguration.setEnabled(true);
      routing().setDiscoveryConfigurationFor("central", centralConfiguration);
    }
    // verify is set, but feature remains disabled
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(false));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(12));
    }
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void checkReleasesHostedHasNoDiscoveryConfiguration() {
    routing().getDiscoveryConfigurationFor("releases");
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void checkPublicGroupHasNoDiscoveryConfiguration() {
    routing().getDiscoveryConfigurationFor("public");
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void checkCentralM1ShadowHasNoDiscoveryConfiguration() {
    routing().getDiscoveryConfigurationFor("central-m1");
  }
}
