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
package org.sonatype.nexus.testsuite.search.nexus778;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryTargetResource;
import org.sonatype.security.rest.model.PrivilegeStatusResource;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test filtering search results based upon security
 */
public class Nexus778SearchResultsFilteringIT
    extends AbstractPrivilegeTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void simpleSearch()
      throws Exception
  {
    List<NexusArtifact> results = searchFor("test1");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);

    results = searchFor("test2");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);
  }

  @Test
  public void filteredSearch()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // First create the targets
    RepositoryTargetResource test1Target =
        createTarget("filterTarget1", Collections.singletonList("/nexus778/test1/.*"));
    RepositoryTargetResource test2Target =
        createTarget("filterTarget2", Collections.singletonList("/nexus778/test2/.*"));

    // Then create the privileges
    PrivilegeStatusResource priv1 = createPrivilege("filterPriv1", test1Target.getId());
    PrivilegeStatusResource priv2 = createPrivilege("filterPriv2", test2Target.getId());

    // Then create the roles
    List<String> combined = new ArrayList<String>();
    combined.add(priv1.getId());
    combined.add(priv2.getId());
    RoleResource role1 = createRole("filterRole1", Collections.singletonList(priv1.getId()));
    RoleResource role2 = createRole("filterRole2", Collections.singletonList(priv2.getId()));
    RoleResource role3 = createRole("filterRole3", combined);

    // Now update the test user
    updateUserRole(TEST_USER_NAME, Collections.singletonList(role3.getId()));

    // Now switch to our newly privileged user and do the search
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to retrieve both test1 & test2 artifacts
    List<NexusArtifact> results = searchFor("test1");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);

    results = searchFor("test2");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);

    // Now update the test user so that the user can only access test1
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    updateUserRole(TEST_USER_NAME, Collections.singletonList(role1.getId()));

    // Now switch to our newly privileged user and do the search
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to retrieve only test1 artifacts
    results = searchFor("test1");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);

    results = searchFor("test2");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 0);

    // Now update the test user so that the user can only access test2
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    updateUserRole(TEST_USER_NAME, Collections.singletonList(role2.getId()));

    // Now switch to our newly privileged user and do the search
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to retrieve only test2 artifacts
    results = searchFor("test1");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 0);

    results = searchFor("test2");
    Assert.assertEquals("Results found " + printResults(results), results.size(), 1);
  }

  private List<NexusArtifact> searchFor(String artifactId)
      throws Exception
  {
    Map<String, String> args = new HashMap<String, String>();
    args.put("a", artifactId);
    args.put("g", "nexus778");

    List<NexusArtifact> results = getSearchMessageUtil().searchFor(args);
    return results;
  }

  private CharSequence printResults(List<NexusArtifact> results) {
    StringBuilder sb = new StringBuilder();
    for (NexusArtifact nexusArtifact : results) {
      sb.append('\n');
      sb.append(nexusArtifact.getGroupId()).append(':').append(nexusArtifact.getArtifactId()).append(':').append(
          nexusArtifact.getVersion());
    }
    return sb;
  }

  private RepositoryTargetResource createTarget(String name, List<String> patterns)
      throws Exception
  {
    RepositoryTargetResource resource = new RepositoryTargetResource();

    resource.setContentClass("maven2");
    resource.setName(name);

    resource.setPatterns(patterns);

    return this.targetUtil.createTarget(resource);
  }

  private PrivilegeStatusResource createPrivilege(String name, String targetId)
      throws Exception
  {
    PrivilegeResource resource = new PrivilegeResource();

    resource.setName(name);
    resource.setDescription("some description");
    resource.setType(TargetPrivilegeDescriptor.TYPE);
    resource.setRepositoryTargetId(targetId);
    resource.addMethod("read");

    return privUtil.createPrivileges(resource).iterator().next();
  }

  private RoleResource createRole(String name, List<String> privilegeIds)
      throws Exception
  {
    RoleResource role = new RoleResource();
    role.setName(name);
    role.setDescription("some description");
    role.setSessionTimeout(60);

    for (String privilegeId : privilegeIds) {
      role.addPrivilege(privilegeId);
    }

    role.addPrivilege("1");
    role.addPrivilege("6");
    role.addPrivilege("14");
    role.addPrivilege("17");
    role.addPrivilege("19");
    role.addPrivilege("44");
    role.addPrivilege("54");
    role.addPrivilege("55");
    role.addPrivilege("57");
    role.addPrivilege("58");
    role.addPrivilege("64");

    return this.roleUtil.createRole(role);
  }

  private void updateUserRole(String username, List<String> roleIds)
      throws Exception
  {
    UserResource resource = userUtil.getUser(username);

    resource.setRoles(roleIds);

    userUtil.updateUser(resource);
  }
}
