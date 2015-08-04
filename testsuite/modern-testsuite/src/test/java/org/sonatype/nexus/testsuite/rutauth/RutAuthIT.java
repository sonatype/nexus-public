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

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.exception.NexusClientAccessForbiddenException;
import org.sonatype.nexus.client.core.exception.NexusClientResponseException;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.security.Users;

import org.junit.Test;

/**
 * Rut Auth related ITs.
 *
 * @since 2.7
 */
public class RutAuthIT
    extends RutAuthITSupport
{

  public RutAuthIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Verify that using admin as a REMOTE_USER, we can access a protected resource that only admin has right to access.
   */
  @Test
  public void accessProtectedResourceViaUserWithPermissions()
      throws Exception
  {
    configureRemoteHeader("REMOTE_USER");
    configureSecurityRealms();

    final NexusClient rutAuthClient = createNexusClientForRemoteHeader("REMOTE_USER", "admin");

    // if we can get the users it means that authentication was successful and we have enough rights
    rutAuthClient.getSubsystem(Users.class).get();
  }

  /**
   * Verify that using deployment as a REMOTE_USER, we can access a protected resource that deployment has rights to
   * access and we cannot access a protected resource that only admin has right to access.
   */
  @Test
  public void accessProtectedResourceViaUserWithoutPermissions()
      throws Exception
  {
    configureRemoteHeader("REMOTE_USER");
    configureSecurityRealms();

    final NexusClient rutAuthClient = createNexusClientForRemoteHeader("REMOTE_USER", "deployment");

    // if we can get the repositories it means that authentication was successful and we have enough rights
    rutAuthClient.getSubsystem(Repositories.class).get();

    thrown.expect(NexusClientAccessForbiddenException.class); // 403
    // we should not be able to access users as we do not have enough rights
    rutAuthClient.getSubsystem(Users.class).get();
  }

  /**
   * Verify that using a user that is not known in the system, we cannot access protected resources.
   */
  @Test
  public void accessProtectedResourceViaUnknownUser()
      throws Exception
  {
    configureRemoteHeader("REMOTE_USER");
    configureSecurityRealms();

    final NexusClient rutAuthClient = createNexusClientForRemoteHeader("REMOTE_USER", "unknown");

    thrown.expect(NexusClientResponseException.class); // 401
    // we should not be able to access repositories as we do not have enough rights
    rutAuthClient.getSubsystem(Repositories.class).get();
  }

}
