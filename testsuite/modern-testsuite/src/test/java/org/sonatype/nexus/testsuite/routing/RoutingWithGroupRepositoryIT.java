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

import java.io.IOException;

import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.subsystem.content.Content.ForceDirective;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Testing automatic routing publishing on group repositories.
 *
 * @author cstamas
 */
public class RoutingWithGroupRepositoryIT
    extends RoutingITSupport
{

  /**
   * Constructor.
   */
  public RoutingWithGroupRepositoryIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  // ==

  private final String REPO_ID = "public";

  private final Location PREFIX_FILE_LOCATION = Location.repositoryLocation(REPO_ID, "/.meta/prefixes.txt");

  protected boolean exists(final Location location)
      throws IOException
  {
    return exists(location, ForceDirective.LOCAL);
  }

  protected boolean noscrape(final Location location)
      throws IOException
  {
    return noscrape(location, ForceDirective.LOCAL);
  }

  @Test
  public void groupMustHaveWLPublishedWhenAllMembersHaveIt()
      throws Exception
  {
    // wait for central
    routingTest().waitForAllRoutingUpdateJobToStop();
    // waitForWLDiscoveryOutcome( "central" );
    // waitForWLPublishingOutcomes( "central", REPO_ID );
    assertThat(exists(PREFIX_FILE_LOCATION), is(true));
    assertThat(noscrape(PREFIX_FILE_LOCATION), is(false));
  }

  @Test
  public void groupLoosesWLIfMemberLooses()
      throws Exception
  {
    routingTest().waitForAllRoutingUpdateJobToStop();
    assertThat(exists(PREFIX_FILE_LOCATION), is(true));
    assertThat(noscrape(PREFIX_FILE_LOCATION), is(false));
    {
      final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
      config.setEnabled(false);
      routing().setDiscoveryConfigurationFor("central", config);
      routingTest().waitForAllRoutingUpdateJobToStop();
      // waitForWLDiscoveryOutcome( "central" );
      // waitForWLPublishingOutcomes( "central", REPO_ID );
    }
    assertThat(exists(PREFIX_FILE_LOCATION), is(true));
    assertThat(noscrape(PREFIX_FILE_LOCATION), is(true));
    {
      final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor("central");
      config.setEnabled(true);
      routing().setDiscoveryConfigurationFor("central", config);
      routingTest().waitForAllRoutingUpdateJobToStop();
      // waitForWLDiscoveryOutcome( "central" );
      // waitForWLPublishingOutcomes( "central", REPO_ID );
    }
    assertThat(exists(PREFIX_FILE_LOCATION), is(true));
    assertThat(noscrape(PREFIX_FILE_LOCATION), is(false));
  }

  @Test(expected = NexusClientBadRequestException.class)
  public void groupWLNotDeletable()
      throws Exception
  {
    // we did no any waiting, e just booted nexus, so it must be present
    assertThat(exists(PREFIX_FILE_LOCATION), is(true));
    assertThat(noscrape(PREFIX_FILE_LOCATION), is(false));
    content().delete(PREFIX_FILE_LOCATION);
  }

}
