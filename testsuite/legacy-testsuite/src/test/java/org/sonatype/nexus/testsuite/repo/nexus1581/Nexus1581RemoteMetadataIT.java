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
package org.sonatype.nexus.testsuite.repo.nexus1581;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.nexus.test.utils.MirrorMessageUtils;
import org.sonatype.nexus.test.utils.TestProperties;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.MediaType;

public class Nexus1581RemoteMetadataIT
    extends AbstractNexusProxyIntegrationTest
{

  public Nexus1581RemoteMetadataIT() {
    super("with-mirror-proxy-repo1581");
  }

  @Test
  public void testGetRemoteMirrorList() throws IOException {
    MirrorMessageUtils mirrorUtils = new MirrorMessageUtils(this.getJsonXStream(), MediaType.APPLICATION_JSON);
    MirrorResourceListResponse response = mirrorUtils.getPredefinedMirrors(this.getTestRepositoryId());

    List<MirrorResource> mirrorResources = response.getData();

    HashMap<String, String> mirrorIdMap = new HashMap<String, String>();

    for (MirrorResource mirrorResource : mirrorResources) {
      mirrorIdMap.put(mirrorResource.getId(), mirrorResource.getUrl());
    }

    Assert.assertTrue(mirrorIdMap.containsKey("mirror1"));
    Assert.assertEquals(mirrorIdMap.get("mirror1"), TestProperties.getString("proxy.repo.base.url") + "/mirror-repo");

    Assert.assertTrue(mirrorIdMap.containsKey("mirror2"));
    Assert.assertEquals(mirrorIdMap.get("mirror2"), TestProperties.getString("proxy.repo.base.url") + "/void");


    Assert.assertEquals(2, mirrorResources.size());
  }


}
