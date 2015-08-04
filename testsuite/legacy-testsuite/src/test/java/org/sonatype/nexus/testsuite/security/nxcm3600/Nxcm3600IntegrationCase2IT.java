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
package org.sonatype.nexus.testsuite.security.nxcm3600;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;

import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Status;

import static org.hamcrest.Matchers.equalTo;

/**
 * Case2 of NXCM-3600: private repository.
 *
 * @author cstamas
 */
public class Nxcm3600IntegrationCase2IT
    extends AbstractNxcm3600IntegrationTest
{
  /**
   * In Case2, we make "private repository" the repository we test against. Notice how it slightly differs from
   * Case1,
   * where 404's are returned instead of 401.
   */
  @Test
  public void testCase2()
      throws IOException
  {
    // make the testing repository "private"
    enablePrivateRepository(TestContainer.getInstance().getTestContext().getAdminUsername(),
        REPO_TEST_HARNESS_RELEASE_REPO);

    Status responseStatus;

    // verify assumptions, we have the stuff deployed and present
    // try and verify authenticate /content GET access gives 200
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(200));
    // try and verify authenticated /service/local GET access gives 200
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(200));
    // try and verify anon /content GET access gives 401
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(401));
    // try and verify anon /service/local GET access gives 401
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(401));

    // now put repository into exposed=false mode
    setExposed(false);

    // READ access
    // try and verify authenticated /content GET access gives 404
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(404));
    // try and verify authenticated /service/local GET access gives 200
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(200));
    // try and verify anon /content GET access gives 404
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(404));
    // try and verify anon /service/local GET access gives 401
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.GET);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(401));

    // DELETE access
    // try and verify authenticated /content DELETE access gives 404
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.DELETE);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(404));
    // try and verify anon /content DELETE access gives 404
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("content/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.DELETE);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(404));
    // try and verify anon /service/local DELETE access gives 401
    responseStatus =
        sendMessage(
            false,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.DELETE);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(401));
    // try and verify authenticated /service/local DELETE access gives 204
    responseStatus =
        sendMessage(
            true,
            RequestFacade.toNexusURL("service/local/repositories/" + REPO_TEST_HARNESS_RELEASE_REPO
                + "/content/nxcm3600/artifact/1.0.0/artifact-1.0.0.jar"), Method.DELETE);
    MatcherAssert.assertThat(responseStatus.getCode(), equalTo(204));
  }
}
