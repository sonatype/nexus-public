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
package org.sonatype.nexus.testsuite.repo.nexus4268;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Method;
import org.restlet.data.Response;

/**
 * This test tests NEXUS-4268 and Nexus' capability to route properly repository types contributed by plugins, hence
 * their role and also implementation comes from core's child classloader (the plugin classloader). For this purpose,
 * nexus-it-helper-plugin got {@link org.sonatype.nexus.plugins.repository.SimpleRepository} repository type, and this
 * IT in it's resources {@code test-config} delivers a configuration that contains this new repository type with id
 * "simple" defined. We test it's reachability over {@code /content/repositories/simple} but also
 * {@code /contenet/simply/simple} since the new repository type defines "simply" as path prefix (the
 * {@code repositories} path prefix is reserved for ALL repositories (by design).
 *
 * @author cstamas
 */
public class Nexus4268NewPluginContributedRepositoryTypeRoutingIT
    extends AbstractNexusIntegrationTest
{
  @Test
  public void testRepositoriesPath()
      throws IOException
  {
    // note the ending slash! We query the repo root, and slash is there to
    // avoid redirect
    Response response = null;

    try {
      final String servicePath = "content/repositories/simple/";

      response = RequestFacade.sendMessage(servicePath, Method.GET);

      Assert.assertEquals("Repository should be accessible over "
          + servicePath, response.getStatus().getCode(), 200);
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }

  @Test
  public void testPathPrefixPath()
      throws IOException
  {
    // note the ending slash! We query the repo root, and slash is there to
    // avoid redirect
    Response response = null;

    try {
      final String servicePath = "content/simply/simple/";

      response = RequestFacade.sendMessage(servicePath, Method.GET);

      Assert.assertEquals("Repository should be accessible over "
          + servicePath, response.getStatus().getCode(), 200);
    }
    finally {
      RequestFacade.releaseResponse(response);
    }
  }
}
