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
package org.sonatype.nexus.testsuite.repo.nexus1730;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.jsecurity.realms.TargetPrivilegeDescriptor;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.PrivilegeResource;
import org.sonatype.nexus.rest.model.RepositoryGroupMemberRepository;
import org.sonatype.nexus.rest.model.RepositoryGroupResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.GroupMessageUtil;
import org.sonatype.nexus.test.utils.PrivilegesMessageUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;

public class Nexus1730DeleteRepoIT
    extends AbstractNexusIntegrationTest
{
  protected PrivilegesMessageUtil privUtil;

  protected RepositoryMessageUtil repoUtil;

  protected GroupMessageUtil groupUtil;

  @Before
  public void prepare()
      throws ComponentLookupException
  {
    privUtil = new PrivilegesMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
    repoUtil = new RepositoryMessageUtil(this, getJsonXStream(), MediaType.APPLICATION_JSON);
    groupUtil = new GroupMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
  }

  @Test
  public void testDeleteRepo()
      throws Exception
  {
    createRepository();
    List<String> privilegeIds = createPrivileges();

    for (String privilegeId : privilegeIds) {
      checkForPrivilege(privilegeId, true);
    }

    deleteRepository();

    for (String privilegeId : privilegeIds) {
      checkForPrivilege(privilegeId, false);
    }
  }

  @Test
  public void testDeleteGroup()
      throws Exception
  {
    createGroup();
    List<String> privilegeIds = createGroupPrivileges();

    for (String privilegeId : privilegeIds) {
      checkForPrivilege(privilegeId, true);
    }

    deleteGroup();

    for (String privilegeId : privilegeIds) {
      checkForPrivilege(privilegeId, false);
    }
  }

  private void createRepository()
      throws Exception
  {
    RepositoryResource repo = new RepositoryResource();
    repo.setId("nexus1730-repo");
    repo.setRepoType("hosted");
    repo.setName("nexus1730-repo");
    repo.setProvider("maven2");
    repo.setFormat("maven2");
    repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
    repoUtil.createRepository(repo);
  }

  private void deleteRepository()
      throws IOException
  {
    repoUtil.sendMessage(Method.DELETE, null, "nexus1730-repo");
  }

  private void createGroup()
      throws IOException
  {
    RepositoryGroupResource group = new RepositoryGroupResource();
    group.setId("nexus1730-group");
    group.setFormat("maven2");
    group.setProvider("maven2");
    group.setName("nexus1730-group");

    RepositoryGroupMemberRepository repo = new RepositoryGroupMemberRepository();
    repo.setId(testRepositoryId);
    group.setRepositories(Arrays.asList(repo));

    groupUtil.createGroup(group);
  }

  private void deleteGroup()
      throws IOException
  {
    RepositoryGroupResource group = new RepositoryGroupResource();
    group.setId("nexus1730-group");

    groupUtil.sendMessage(Method.DELETE, group);
  }

  private List<String> createPrivileges()
      throws Exception
  {
    PrivilegeResource priv = new PrivilegeResource();
    priv.setDescription("nexus1730-priv");
    priv.setMethod(Arrays.asList("read", "delete", "create", "update"));
    priv.setRepositoryId("nexus1730-repo");
    priv.setRepositoryTargetId("1");
    priv.setType(TargetPrivilegeDescriptor.TYPE);
    priv.setName("nexus1730-priv");
    List<PrivilegeStatusResource> privs = privUtil.createPrivileges(priv);

    List<String> privIds = new ArrayList<String>();

    for (PrivilegeStatusResource privilege : privs) {
      privIds.add(privilege.getId());
    }

    return privIds;
  }

  private List<String> createGroupPrivileges()
      throws IOException
  {
    PrivilegeResource priv = new PrivilegeResource();
    priv.setDescription("nexus1730-priv");
    priv.setMethod(Arrays.asList("read", "delete", "create", "update"));
    priv.setRepositoryGroupId("nexus1730-group");
    priv.setRepositoryTargetId("1");
    priv.setType(TargetPrivilegeDescriptor.TYPE);
    priv.setName("nexus1730-priv");
    List<PrivilegeStatusResource> privs = privUtil.createPrivileges(priv);

    List<String> privIds = new ArrayList<String>();

    for (PrivilegeStatusResource privilege : privs) {
      privIds.add(privilege.getId());
    }

    return privIds;
  }

  private boolean checkForPrivilege(String id, boolean shouldFind)
      throws Exception
  {
    Response response = privUtil.sendMessage(Method.GET, null, id);
    if (response.getStatus().isSuccess() != shouldFind) {
      Assert.fail("Privilege " + id + " should " + (shouldFind ? "" : " not ") + "have been found");
    }

    return true;
  }
}
