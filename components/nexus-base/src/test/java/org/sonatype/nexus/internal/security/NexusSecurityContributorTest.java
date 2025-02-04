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
package org.sonatype.nexus.internal.security;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;

import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class NexusSecurityContributorTest
    extends TestSupport
{
  private NexusSecurityContributor underTest;

  @Before
  public void setup() {
    underTest = new NexusSecurityContributor();
  }

  @Test
  public void testGetContribution() {
    MemorySecurityConfiguration config = underTest.getContribution();
    assertThat(config.getUsers().size(), is(0));
    assertThat(config.getUserRoleMappings().size(), is(0));
    assertThat(config.getRoles().size(), is(2));
    assertThat(config.getRole("nx-admin").getPrivileges(), containsInAnyOrder("nx-all"));
    assertThat(config.getRole("nx-anonymous").getPrivileges(),
        containsInAnyOrder("nx-search-read", "nx-healthcheck-read", "nx-repository-view-*-*-browse",
            "nx-repository-view-*-*-read"));
    assertThat(config.getPrivileges().stream().map(CPrivilege::getId).collect(toList()),
        containsInAnyOrder("nx-all",
            "nx-component-upload",
            "nx-search-read",
            "nx-bundles-all", "nx-bundles-read",
            "nx-settings-all", "nx-settings-read", "nx-settings-update",
            "nx-apikey-all",
            "nx-userschangepw",
            "nx-users-all", "nx-users-create", "nx-users-read", "nx-users-update", "nx-users-delete",
            "nx-roles-all", "nx-roles-create", "nx-roles-read", "nx-roles-update", "nx-roles-delete",
            "nx-privileges-all", "nx-privileges-create", "nx-privileges-read", "nx-privileges-update",
            "nx-privileges-delete"));
  }
}
