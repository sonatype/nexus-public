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

import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;
import org.sonatype.nexus.client.core.subsystem.routing.Status;
import org.sonatype.nexus.client.core.subsystem.routing.Status.Outcome;
import org.sonatype.sisu.litmus.testsupport.group.Smoke;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Simple smoke IT for Routing REST being responsive and is reporting the expected statuses.
 *
 * @author cstamas
 */
@Category(Smoke.class)
public class RoutingSmokeIT
    extends RoutingITSupport
{

  /**
   * Constructor.
   */
  public RoutingSmokeIT(final String nexusBundleCoordinates) {
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
  public void checkPublicGroupResponse() {
    // public
    final Status publicStatus = routing().getStatus("public");
    assertThat(publicStatus.getPublishedStatus(), equalTo(Outcome.SUCCEEDED));
  }

  @Test
  public void checkReleasesHostedResponse() {
    // releases
    final Status releasesStatus = routing().getStatus("releases");
    assertThat(releasesStatus.getPublishedStatus(), equalTo(Outcome.SUCCEEDED));
    assertThat(releasesStatus.getDiscoveryStatus(), is(nullValue()));
  }

  @Test
  public void checkCentralProxyResponse() {
    // central
    final Status centralStatus = routing().getStatus("central");
    assertThat(centralStatus.getPublishedStatus(), equalTo(Outcome.SUCCEEDED));
    assertThat(centralStatus.getDiscoveryStatus(), is(notNullValue()));
    assertThat(centralStatus.getDiscoveryStatus().getDiscoveryLastStatus(), equalTo(Outcome.SUCCEEDED));
  }

  @Test
  public void checkCentralProxyConfiguration() {
    // get configuration for central and check for sane values (actually, they should be defaults).
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(true));
      assertThat(centralConfiguration.getIntervalHours(), equalTo(24));
      // checked ok, set interval to 12h
      centralConfiguration.setIntervalHours(12);
      routing().setDiscoveryConfigurationFor("central", centralConfiguration);
    }
    // verify is set
    {
      final DiscoveryConfiguration centralConfiguration = routing().getDiscoveryConfigurationFor("central");
      assertThat(centralConfiguration, is(notNullValue()));
      assertThat(centralConfiguration.isEnabled(), equalTo(true));
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

  @Test(expected = NexusClientBadRequestException.class)
  public void checkDiscoveryOnOutOfServiceRepository() {
    try {
      repositories().get("central").putOutOfService().save();
      routing().updatePrefixFile("central");
    }
    finally {
      repositories().get("central").putInService().save();
    }
  }

  @Test
  public void checkDiscoveryOnBlockedProxyRepository()
      throws InterruptedException
  {
    try {
      final Status statusBefore = routing().getStatus("central");
      assertThat(statusBefore.getPublishedStatus(), equalTo(Outcome.SUCCEEDED));
      assertThat(statusBefore.getDiscoveryStatus().getDiscoveryLastStatus(), equalTo(Outcome.SUCCEEDED));

      // block it
      repositories().get(ProxyRepository.class, "central").block().save();
      routing().updatePrefixFile("central");
      routingTest().waitForAllRoutingUpdateJobToStop();
      // waitForWLDiscoveryOutcome( "central" );

      // recheck
      final Status statusAfter = routing().getStatus("central");
      assertThat(statusAfter.getPublishedStatus(), equalTo(Outcome.SUCCEEDED));
      assertThat(statusAfter.getDiscoveryStatus().getDiscoveryLastStatus(), equalTo(Outcome.FAILED));
      assertThat(statusAfter.getDiscoveryStatus().getDiscoveryLastMessage(), containsString("blocked"));
    }
    finally {
      repositories().get(ProxyRepository.class, "central").unblock().save();
    }
  }

}
