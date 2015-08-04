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
package org.sonatype.nexus.testsuite.rutauth;

import java.util.List;

import org.sonatype.nexus.client.core.subsystem.config.Security;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Rut Auth related ITs.
 *
 * @since 2.7
 */
public class RutAuthAutoEnableRealmIT
    extends RutAuthITSupport
{

  public RutAuthAutoEnableRealmIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Verify that when authentication capability is created/enabled, Ruth Auth realm is automatically added to the list
   * of configured realms.
   */
  @Test
  public void automaticallyEnableRealmWhenCapabilityCreated()
      throws Exception
  {
    // check that realm is added
    Security security = serverConfiguration().security();
    assertThat(security.settings().getRealms(), not(hasItem(RUTAUTH_REALM)));
    configureRemoteHeader("REMOTE_USER");
    assertThat(security.refresh().settings().getRealms(), hasItem(RUTAUTH_REALM));
    // check that we do not add the realm once more when we update
    List<String> realmsBeforeUpdate = security.refresh().settings().getRealms();
    configureRemoteHeader("REMOTE_AUTH");
    assertThat(
        security.refresh().settings().getRealms(),
        containsInAnyOrder(realmsBeforeUpdate.toArray(new String[realmsBeforeUpdate.size()]))
    );
  }

}
