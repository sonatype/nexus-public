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
package org.sonatype.nexus.testsuite.artifact.nexus3615;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.ArtifactInfoResource;
import org.sonatype.nexus.test.utils.RoleMessageUtil;
import org.sonatype.nexus.test.utils.UserMessageUtil;
import org.sonatype.security.rest.model.RoleResource;
import org.sonatype.security.rest.model.UserResource;

import com.thoughtworks.xstream.XStream;
import org.apache.maven.index.artifact.Gav;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.integrationtests.AbstractPrivilegeTest.TEST_USER_NAME;
import static org.sonatype.nexus.integrationtests.AbstractPrivilegeTest.TEST_USER_PASSWORD;

public class Nexus3615ArtifactInfoSecurityIT
    extends AbstractArtifactInfoIT
{

  protected UserMessageUtil userUtil;

  protected RoleMessageUtil roleUtil;

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Before
  public void setUp() {
    XStream xstream = this.getXMLXStream();
    this.userUtil = new UserMessageUtil(this, xstream, MediaType.APPLICATION_XML);
    this.roleUtil = new RoleMessageUtil(this, xstream, MediaType.APPLICATION_XML);
  }

  protected void giveUserRole(String userId, String roleId, boolean overwrite)
      throws IOException
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    // add it
    UserResource testUser = this.userUtil.getUser(userId);
    if (overwrite) {
      testUser.getRoles().clear();
    }
    testUser.addRole(roleId);
    this.userUtil.updateUser(testUser);
  }

  protected void giveUserPrivilege(String userId, String priv)
      throws IOException
  {
    // use admin
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    RoleResource role = null;

    // first try to retrieve
    for (RoleResource roleResource : roleUtil.getList()) {
      if (roleResource.getName().equals(priv + "Role")) {
        role = roleResource;

        if (!role.getPrivileges().contains(priv)) {
          role.addPrivilege(priv);
          // update the permissions
          RoleMessageUtil.update(role);
        }
        break;
      }
    }

    if (role == null) {
      // now give create
      role = new RoleResource();
      role.setDescription(priv + " Role");
      role.setName(priv + "Role");
      role.setSessionTimeout(60);
      role.addPrivilege(priv);
      // save it
      role = this.roleUtil.createRole(role);
    }

    // add it
    this.giveUserRole(userId, role.getId(), false);
  }

  @Test
  public void checkViewAccess()
      throws Exception
  {
    // force re-indexing to ensure that our artifact will be found by artifact info
    final Gav gav = new Gav("nexus3615", "artifact", "1.0");
    getSearchMessageUtil().reindexGAV(REPO_TEST_HARNESS_RELEASE_REPO, gav);
    getSearchMessageUtil().reindexGAV(REPO_TEST_HARNESS_REPO2, gav);
    getSearchMessageUtil().reindexGAV(REPO_TEST_HARNESS_REPO, gav);

    this.giveUserRole(TEST_USER_NAME, "ui-search", true);
    this.giveUserPrivilege(TEST_USER_NAME, "T1"); // all m2 repo, read
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + REPO_TEST_HARNESS_REPO);

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    final ArtifactInfoResource info = getSearchMessageUtil().getInfo(
        REPO_TEST_HARNESS_REPO, "nexus3615/artifact/1.0/artifact-1.0.jar"
    );

    assertThat(info.getRepositoryId(), is(REPO_TEST_HARNESS_REPO));
    assertThat(info.getRepositoryPath(), is("/nexus3615/artifact/1.0/artifact-1.0.jar"));
    assertThat(info.getSha1Hash(), is("b354a0022914a48daf90b5b203f90077f6852c68"));
    // view priv no longer controls search results, only read priv
    assertThat(info.getRepositories().size(), is(3));
    assertThat(getRepositoryId(info.getRepositories()), hasItems(REPO_TEST_HARNESS_REPO));
    assertThat(info.getMimeType(), is("application/java-archive"));
    assertThat(info.getSize(), is(1364L));
  }

}
