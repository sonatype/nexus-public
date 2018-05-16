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
package org.sonatype.nexus.testsuite.p2.nxcm2812;

import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.test.http.RemoteRepositories;
import org.sonatype.nexus.testsuite.p2.AbstractNexusProxyP2IT;

import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.tests.http.server.fluent.Behaviours.error;

/**
 * This IT checks that previously retrieved P2 metadata is used from cache, even when the proxied P2 repository is
 * unavailable.
 * (Nexus is configured to always go remote for the P2 proxy repository.)
 */
public class NXCM2812UseCachedMetadataWhenProxyIsUnavailableIT
    extends AbstractNexusProxyP2IT
{

  public NXCM2812UseCachedMetadataWhenProxyIsUnavailableIT() {
    super("nxcm2812");
  }

  @Test
  public void test()
      throws Exception
  {
    final String url = "content/repositories/" + getTestRepositoryId() + "/content.xml";

    // init local storage
    Response content = null;
    String metadataBefore;
    try {
      content = RequestFacade.sendMessage(url, Method.GET);
      metadataBefore = content.getEntity().getText();
    }
    finally {
      RequestFacade.releaseResponse(content);
    }

    assertThat(metadataBefore, containsString("<?metadataRepository"));

    // invalidate remote repo
    replaceProxy();

    // check delivery from local storage
    String metadataAfter;
    try {
      content = RequestFacade.sendMessage(url, Method.GET);
      metadataAfter = content.getEntity().getText();
    }
    finally {
      RequestFacade.releaseResponse(content);
    }

    assertThat(metadataAfter, containsString("<?metadataRepository"));
    assertThat(metadataAfter, is(equalTo(metadataBefore)));
  }

  private void replaceProxy()
      throws Exception
  {
    int port = remoteRepositories.getPort();
    remoteRepositories.stop();

    remoteRepositories = RemoteRepositories.builder().port(port).behave("/*", error(503)).build().start();
  }

}
