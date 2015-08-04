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
package org.sonatype.nexus.testsuite.p2.nxcm1719;

import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.integrationtests.RequestFacade.doGetRequest;
import static org.sonatype.nexus.test.utils.TaskScheduleUtil.waitForAllTasksToStop;

public class NXCM1719UpdateSiteProxyIT
    extends AbstractNexusProxyP2IT
{

  public NXCM1719UpdateSiteProxyIT() {
    super("nxcm1719");
  }

  @Test
  public void test()
      throws Exception
  {
    {
      final Response response = doGetRequest("content/repositories/" + getTestRepositoryId() + "/features/");
      assertThat(response.getStatus().isSuccess(), is(false));
    }

    final RepositoryMessageUtil repoUtil = new RepositoryMessageUtil(
        this, getXMLXStream(), MediaType.APPLICATION_XML
    );
    final RepositoryProxyResource repo = (RepositoryProxyResource) repoUtil.getRepository(getTestRepositoryId());
    repo.getRemoteStorage().setRemoteStorageUrl(TestProperties.getString("proxy-repo-base-url") + "nxcm1719/");
    repoUtil.updateRepo(repo);

    waitForAllTasksToStop();

    {
      final Response response = doGetRequest("content/repositories/" + getTestRepositoryId() + "/features/");
      assertThat(response.getStatus().isSuccess(), is(true));
    }

    installAndVerifyP2Feature();
  }

}
