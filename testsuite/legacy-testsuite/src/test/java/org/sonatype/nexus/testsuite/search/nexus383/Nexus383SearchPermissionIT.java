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
package org.sonatype.nexus.testsuite.search.nexus383;

import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.NexusArtifact;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the privilege for search operations.
 */
public class Nexus383SearchPermissionIT
    extends AbstractPrivilegeTest
{
  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void withPermission()
      throws Exception
  {
    if (printKnownErrorButDoNotFail(Nexus383SearchPermissionIT.class, "withPermission")) {
      return;
    }

    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-search", "1", "2" /* login */, "6", "14",
        "17" /* search */, "19", "44", "54", "55", "57", "58", "59", "T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // Should be able to find artifacts
    List<NexusArtifact> results = getSearchMessageUtil().searchFor("nexus383");
    Assert.assertEquals(2, results.size());
  }

  @Test
  public void withoutSearchPermission()
      throws Exception
  {
    if (printKnownErrorButDoNotFail(Nexus383SearchPermissionIT.class, "withoutSearchPermission")) {
      return;
    }

    overwriteUserRole(TEST_USER_NAME, "anonymous-with-login-but-search", "1", "2" /* login */, "6", "14", "19",
        /* "17" search, */"44", "54", "55", "57", "58", "59", "T1", "T2");

    TestContainer.getInstance().getTestContext().setUsername(TEST_USER_NAME);
    TestContainer.getInstance().getTestContext().setPassword(TEST_USER_PASSWORD);

    // NOT Should be able to find artifacts
    HashMap<String, String> queryArgs = new HashMap<String, String>();

    queryArgs.put("q", "nexus383");

    getSearchMessageUtil().searchFor(queryArgs, NexusRequestMatchers.respondsWithStatusCode(401));
  }

  // @Test
  // public void withoutRepositoryReadPermission()
  // throws Exception
  // {
  // overwriteUserRole( TEST_USER_NAME, "anonymous-with-login-but-repo", "1", "2" /* login */, "6", "14", "19",
  // "17", "44", "54", "55", "57", "58", "59"/* , "T1", "T2" */);
  //
  // TestContainer.getInstance().getTestContext().setUsername( TEST_USER_NAME );
  // TestContainer.getInstance().getTestContext().setPassword( TEST_USER_PASSWORD );
  //
  // // Should found nothing
  // List<NexusArtifact> results = messageUtil.searchFor( "nexus383" );
  // Assert.assertEquals( 0, results.size() );
  //
  // }
}
