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
package org.sonatype.nexus.testsuite.security.nexus1286;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.security.rest.model.ExternalRoleMappingResource;
import org.sonatype.security.rest.model.PlexusRoleResource;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus1286RoleListIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void invalidSource()
      throws IOException
  {
    String uriPart = RequestFacade.SERVICE_LOCAL + "plexus_roles/" + "INVALID";

    RequestFacade.doGet(uriPart, respondsWithStatusCode(404));
  }

  @Test
  public void defaultSourceRoles()
      throws IOException
  {
    RoleMessageUtil roleUtil = new RoleMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusRoleResource> roles = roleUtil.getRoles("default");

    Set<String> ids = this.getRoleIds(roles);
    Assert.assertTrue(ids.contains("nx-admin"));
    Assert.assertTrue(ids.contains("anonymous"));

  }

  @Test
  public void allSourceRoles()
      throws IOException
  {
    RoleMessageUtil roleUtil = new RoleMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<PlexusRoleResource> roles = roleUtil.getRoles("all");

    Set<String> ids = this.getRoleIds(roles);
    Assert.assertTrue(ids.contains("nx-admin"));
    Assert.assertTrue(ids.contains("anonymous"));
  }

  public void getdefaultExternalRoleMap()
      throws IOException
  {
    RoleMessageUtil roleUtil = new RoleMessageUtil(this, this.getJsonXStream(), MediaType.APPLICATION_JSON);
    List<ExternalRoleMappingResource> roles = roleUtil.getExternalRoleMap("all");
    Assert.assertEquals(0, roles.size());

    roles = roleUtil.getExternalRoleMap("default");
    Assert.assertEquals(0, roles.size());
  }

  private Set<String> getRoleIds(List<PlexusRoleResource> roles) {
    Set<String> ids = new HashSet<String>();
    for (PlexusRoleResource role : roles) {
      ids.add(role.getRoleId());
    }
    return ids;
  }

}
