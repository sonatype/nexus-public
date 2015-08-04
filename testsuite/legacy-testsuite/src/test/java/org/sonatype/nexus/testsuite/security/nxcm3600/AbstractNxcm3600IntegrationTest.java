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
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * See NXCM-3600 issue for test description.
 *
 * @author cstamas
 */
public class AbstractNxcm3600IntegrationTest
    extends AbstractPrivilegeTest
{
  private final RepositoryMessageUtil repositoryMessageUtil;

  public AbstractNxcm3600IntegrationTest() {
    super(REPO_TEST_HARNESS_RELEASE_REPO);
    this.repositoryMessageUtil = new RepositoryMessageUtil(this, getXMLXStream(), MediaType.APPLICATION_XML);
  }

  protected RepositoryMessageUtil getRepositoryMessageUtil() {
    return repositoryMessageUtil;
  }

  /**
   * Sets the exposed flag of repository.
   */
  protected void setExposed(final boolean exposed)
      throws IOException
  {
    TestContainer.getInstance().getTestContext().useAdminForRequests();
    TestContainer.getInstance().getTestContext().setSecureTest(true);
    final RepositoryBaseResource releasesRepository =
        getRepositoryMessageUtil().getRepository(REPO_TEST_HARNESS_RELEASE_REPO);
    releasesRepository.setExposed(exposed);
    getRepositoryMessageUtil().updateRepo(releasesRepository);
  }

  protected Status sendMessage(final boolean authenticated, final URL url, Method method)
      throws IOException
  {
    Response response = null;

    final boolean wasSecureTest = TestContainer.getInstance().getTestContext().isSecureTest();

    try {
      TestContainer.getInstance().getTestContext().setSecureTest(authenticated);

      response = RequestFacade.sendMessage(url, method, null);

      return response.getStatus();
    }
    finally {
      if (response != null) {
        RequestFacade.releaseResponse(response);
      }

      TestContainer.getInstance().getTestContext().setSecureTest(wasSecureTest);
    }
  }
}
