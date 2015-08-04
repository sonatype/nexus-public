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
package org.sonatype.nexus.proxy;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;

import org.junit.Test;

public class RepositoryExpireCacheTest
    extends AbstractProxyTestEnvironment
{

  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    return jettyTestsuiteEnvironmentBuilder;
  }

  protected Repository getRepository()
      throws NoSuchResourceStoreException
  {
    Repository repo1 = getRepositoryRegistry().getRepository("repo1");

    repo1.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE);

    return repo1;

  }

  public void retrieveItem()
      throws Exception
  {
    StorageItem item = getRepository().retrieveItem(
        new ResourceStoreRequest("/activemq/activemq-core/1.2/activemq-core-1.2.jar", false));

    checkForFileAndMatchContents(item);
  }

  @Test
  public void testSimple()
      throws Exception
  {
    // make a bad request
    ResourceStoreRequest req = new ResourceStoreRequest(
        "/activemq/activemq-core/1.2/activemq-core-1.2.jar-no-such",
        false);

    try {
      getRepository().retrieveItem(req);
    }
    catch (ItemNotFoundException e) {
      // good, but now we have NFC filled with stuff
    }

    // make another bad request
    req = new ResourceStoreRequest("/activemq1/activemq-core/1.2/activemq-core-1.2.jar-no-such", false);

    try {
      getRepository().retrieveItem(req);
    }
    catch (ItemNotFoundException e) {
      // good, but now we have NFC filled with stuff
    }

    // we have now two items in NFC
    assertEquals(2L, getRepository().getNotFoundCache().getStatistics().getSize());

    // remove one
    getRepository().expireCaches(new ResourceStoreRequest("/activemq1/activemq-core", true));

    assertEquals(1L, getRepository().getNotFoundCache().getStatistics().getSize());

    getRepository().expireCaches(new ResourceStoreRequest("/", true));

    assertEquals(0L, getRepository().getNotFoundCache().getStatistics().getSize());

    retrieveItem();
  }

}
