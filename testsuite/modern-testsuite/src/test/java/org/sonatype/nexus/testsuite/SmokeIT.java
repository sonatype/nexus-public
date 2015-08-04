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
package org.sonatype.nexus.testsuite;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.NexusStatus;
import org.sonatype.sisu.litmus.testsupport.group.Smoke;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Most basic IT just checking is bundle alive at all.
 *
 * @since 2.4
 */
@Category(Smoke.class)
public class SmokeIT
    extends NexusCoreITSupport
{
  /**
   * Constructor.
   */
  public SmokeIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Test that only verifies that Nexus reports itself (the status resource actually, used by {@link NexusClient}) as
   * expected.
   */
  @Test
  public void verifyNexusReportsAsHealthyAndCorrect() {
    final NexusStatus nexusStatus = client().getStatus();
    assertThat(nexusStatus, is(notNullValue()));
    assertThat(nexusStatus.isFirstStart(), is(true)); // should be true
    assertThat(nexusStatus.isInstanceUpgraded(), is(false)); // should be false
    // TODO: Need a generic way to detect the version of the bundle being runned.
    // This below would work with parametrized coordinates, but does not work with "normal" use
    // when DM is used as I have no version it seems, and it's known only in the moment of
    // resolving Nexus GA, but it's seems it's not stored/exposed anywhere.
    // final Artifact nexusBundleArtifact = new DefaultArtifact( nexusBundleCoordinates );
    // assertThat( nexusStatus.getVersion(), is( nexus().getConfiguration(). nexusBundleArtifact.getBaseVersion() ) ); // version
    assertThat(nexusStatus.getEditionShort(), equalTo("OSS"));
  }

}
