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
package org.sonatype.nexus.testsuite.security.nexus1599;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.data.MediaType;

/**
 * @author juven
 */
public class Nexus1599ViewPrivilegeIT
    extends AbstractPrivilegeTest
{

  protected RepositoryMessageUtil repoMsgUtil;

  public Nexus1599ViewPrivilegeIT()
      throws Exception
  {
    super(REPO_TEST_HARNESS_REPO);

    this.repoMsgUtil = new RepositoryMessageUtil(
        this, this.getJsonXStream(),
        MediaType.APPLICATION_JSON);
  }

  @BeforeClass
  public static void enableSecureContext() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void testSearch()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-search");
    this.giveUserPrivilege(TEST_USER_NAME, "T1"); // all m2 repo, read

    // without view privilege
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertEquals(1, getSearchMessageUtil().searchFor(getTestId()).size());

    // with view privilege
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + REPO_TEST_HARNESS_REPO);
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertEquals(1, getSearchMessageUtil().searchFor(getTestId()).size());

    this.removePrivilege(TEST_USER_NAME, "T1");
  }

  @Test
  public void testBrowseFeed()
      throws Exception
  {
    this.giveUserRole(TEST_USER_NAME, "ui-system-feeds");
    this.giveUserPrivilege(TEST_USER_NAME, "T1"); // all m2 repo, read

    // without view privilege
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertFalse(FeedUtil.getFeed("recentlyChangedArtifacts").getEntries().isEmpty());

    // with view privilege
    this.giveUserPrivilege(TEST_USER_NAME, "repository-" + REPO_TEST_HARNESS_REPO);
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertFalse(FeedUtil.getFeed("recentlyChangedArtifacts").getEntries().isEmpty());

    this.removePrivilege(TEST_USER_NAME, "T1");
  }

  @Test
  public void testBrowseRepository()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();

    List<RepositoryListResource> repos = repoMsgUtil.getList();

    Assert.assertTrue(!repos.isEmpty());

    for (RepositoryListResource repo : repos) {
      assertViewPrivilege(repo.getId());
    }
  }

  private void assertViewPrivilege(String repoId)
      throws Exception
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    addPrivilege(TEST_USER_NAME, "repository-" + repoId);
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertTrue(containsRepo(repoMsgUtil.getList(), repoId));

    TestContainer.getInstance().getTestContext().useAdminForRequests();
    removePrivilege(TEST_USER_NAME, "repository-" + repoId);
    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    Assert.assertFalse("Repo '" + repoId + "' should be hidden!", containsRepo(repoMsgUtil.getList(), repoId));
  }

  private boolean containsRepo(List<RepositoryListResource> repos, String repoId) {
    for (RepositoryListResource repo : repos) {
      if (repo.getId().equals(repoId)) {
        return true;
      }
    }
    return false;
  }

}
