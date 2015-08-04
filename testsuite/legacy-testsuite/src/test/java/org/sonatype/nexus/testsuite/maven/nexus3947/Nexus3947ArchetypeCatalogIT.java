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
package org.sonatype.nexus.testsuite.maven.nexus3947;

import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class Nexus3947ArchetypeCatalogIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void testArchetypeCatalog()
      throws Exception
  {
    Response response;

    ArchetypeCatalog catalog;

    ArchetypeCatalogXpp3Reader acr = new ArchetypeCatalogXpp3Reader();

    // path of catalog
    String relativePath = "archetype-catalog.xml";
    String url = getRepositoryUrl(getTestRepositoryId()) + relativePath;

    // request the catalog
    response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    // read and check
    catalog = acr.read(response.getEntity().getReader());
    Assert.assertEquals(catalog.getArchetypes().size(), 1);

    // deploy one new archetype
    int httpResponseCode =
        getDeployUtils().deployUsingPomWithRest(getTestRepositoryId(), getTestFile("simple-archetype2.jar"),
            getTestFile("simple-archetype2.pom"), null, null);
    Assert.assertTrue("Unable to deploy artifact " + httpResponseCode, Status.isSuccess(httpResponseCode));

    // wait
    getEventInspectorsUtil().waitForCalmPeriod();

    // request the catalog
    response = RequestFacade.sendMessage(new URL(url), Method.GET, null);

    // read and check
    catalog = acr.read(response.getEntity().getReader());
    Assert.assertEquals(catalog.getArchetypes().size(), 2);
  }
}
