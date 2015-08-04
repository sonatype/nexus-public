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
package org.sonatype.nexus.testsuite.proxy.nexus5125;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;

import org.junit.Test;
import org.restlet.data.Status;

import static org.sonatype.nexus.integrationtests.RequestFacade.doGet;

/**
 * Verifies that index pages of a remote repository is not delivered from a proxy repository.
 */
public class Nexus5125RemoteIndexPage404IT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void testIndexPageIs404()
      throws IOException
  {
    // nexus storage is empty and we're hitting a directory on the proxy server -> 404
    doGet("content/repositories/" + getTestRepositoryId() + "/nexus5125/artifact/1.0/",
        NexusRequestMatchers.respondsWithStatus(Status.CLIENT_ERROR_NOT_FOUND));

    // expect artifact from proxy server -> 200
    doGet("content/repositories/" + getTestRepositoryId() + "/nexus5125/artifact/1.0/artifact-1.0.jar",
        NexusRequestMatchers.respondsWithStatus(Status.SUCCESS_OK));

    // nexus storage has artifact in path 'below', we expect index page -> 200
    doGet("content/repositories/" + getTestRepositoryId() + "/nexus5125/artifact/1.0/",
        NexusRequestMatchers.respondsWithStatus(Status.SUCCESS_OK));
  }

}
