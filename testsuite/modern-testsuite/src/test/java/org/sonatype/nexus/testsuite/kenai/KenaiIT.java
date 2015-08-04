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
package org.sonatype.nexus.testsuite.kenai;

import java.util.Collection;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.exception.NexusClientResponseException;
import org.sonatype.nexus.client.core.subsystem.security.Role;
import org.sonatype.nexus.client.core.subsystem.security.Roles;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * This is a codified version of "smoke testing Kenai Realm", see below the wiki link.
 *
 * @author cstamas
 * @see <a href="https://docs.sonatype.com/display/Nexus/Testing+Kenai+%28java.net%29+Security+Realm">Testing Kenai
 *      Realm</a> wiki page
 */
public class KenaiIT
    extends KenaiITSupport
{

  public KenaiIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  protected void createKenaiBaseRoleIfNeeded() {
    final Roles roles = client().getSubsystem(Roles.class);
    try {
      roles.get("kenai-base-role");
    }
    catch (NexusClientNotFoundException e) {
      roles.create("kenai-base-role")
          .withName("Kenai Base Role")
          .withDescription("Kenai Base Role")
          .withRole("nx-admin")
          .save();
    }
  }

  @Test
  public void kenaiLogsInWithGoodCredentials()
      throws Exception
  {
    createKenaiBaseRoleIfNeeded();
    // see KenaiAuthcBehaviour: "authenticated" users are those having password = username + "123"
    final NexusClient kenaiAuthenticatedClient = createNexusClient(nexus(), "kenaiuser", "kenaiuser123");
    final Roles kenaiAuthenticatedRoles = kenaiAuthenticatedClient.getSubsystem(Roles.class);
    final Collection<Role> existingRoles = kenaiAuthenticatedRoles.get();
    // most likely redundant, as it all this above would not work, a NexusClientResponseException 401 would be
    // thrown at kenaiAuthenticatedRoles.get();
    assertThat(existingRoles, not(empty()));
  }

  @Test(expected = NexusClientResponseException.class)
  public void kenaiNotLogsInWithBadCredentials()
      throws Exception
  {
    createKenaiBaseRoleIfNeeded();
    // see KenaiAuthcBehaviour: "authenticated" users are those having password = username + "123"
    // this user below will have BAD credentials
    final NexusClient kenaiAuthenticatedClient = createNexusClient(nexus(), "kenaiuser", "kenaiuserABC");
    assertThat("Line above should throw NexusClientResponseException with 401 response!", false);
  }
}
