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
package org.sonatype.nexus.testsuite.index.nexus3039;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Test;
import org.restlet.data.MediaType;

import static org.sonatype.nexus.test.utils.ResponseMatchers.respondsWithStatusCode;

public class Nexus3039IndexTreeIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void testIndexTree()
      throws Exception
  {
    String repoId = this.getTestRepositoryId();

    // get the index tree
    RequestFacade.doGet(RequestFacade.SERVICE_LOCAL + "repositories/" + repoId + "/index_content/");

    RepositoryMessageUtil repoUtil =
        new RepositoryMessageUtil(this, this.getXMLXStream(), MediaType.APPLICATION_XML);

    RepositoryResource resource = (RepositoryResource) repoUtil.getRepository(repoId);
    resource.setIndexable(false);
    repoUtil.updateRepo(resource);

    // get the index tree
    RequestFacade.doGet(RequestFacade.SERVICE_LOCAL + "repositories/" + repoId + "/index_content/",
        respondsWithStatusCode(404));

  }

  @Test
  public void testGroupIndexTree()
      throws Exception
  {
    String repoId = "public";

    // get the index tree
    RequestFacade.doGet(RequestFacade.SERVICE_LOCAL + "repo_groups/" + repoId + "/index_content/");
  }

}
