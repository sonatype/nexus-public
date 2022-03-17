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
package org.sonatype.nexus.testsuite.index.nexus1961;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNode;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeNodeDTO;
import org.sonatype.nexus.rest.indextreeview.IndexBrowserTreeViewResponseDTO;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.XStreamFactory;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus1961IndexContentIT
    extends AbstractNexusIntegrationTest
{

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    RepositoryMessageUtil.updateIndexes(REPO_TEST_HARNESS_REPO);
  }

  @Test
  public void getIndexContent()
      throws Exception
  {
    String serviceURI = "service/local/repositories/" + REPO_TEST_HARNESS_REPO + "/index_content/";

    String responseText = RequestFacade.doGetForText(serviceURI);

    XStream xstream = XStreamFactory.getXmlXStream();

    Class[] indexBrowserTypes = new Class[]{
        IndexBrowserTreeNode.class,
        IndexBrowserTreeViewResponseDTO.class
    };
    xstream.allowTypes(indexBrowserTypes);
    xstream.processAnnotations(indexBrowserTypes);

    XStreamRepresentation re = new XStreamRepresentation(xstream, responseText, MediaType.APPLICATION_XML);
    IndexBrowserTreeViewResponseDTO resourceResponse =
        (IndexBrowserTreeViewResponseDTO) re.getPayload(new IndexBrowserTreeViewResponseDTO());

    IndexBrowserTreeNodeDTO content = resourceResponse.getData();

    for (IndexBrowserTreeNodeDTO child : content.getChildren()) {
      Assert.assertEquals(child.getNodeName(), "nexus1961");
    }
  }
}
