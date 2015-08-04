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

import java.io.ByteArrayInputStream;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Test;

public class LinkTest
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

  @Test
  public void testRepoLinks()
      throws Exception
  {
    String contentString = "SOME_CONTENT";

    Repository repo1 = getRepositoryRegistry().getRepository("repo1");

    DefaultStorageFileItem file = new DefaultStorageFileItem(
        repo1,
        new ResourceStoreRequest("/a.txt"),
        true,
        true,
        new StringContentLocator(contentString));
    file.getRepositoryItemAttributes().put("attr1", "ATTR1");
    repo1.storeItem(false, file);

    DefaultStorageLinkItem link = new DefaultStorageLinkItem(repo1, new ResourceStoreRequest("/b.txt"), true, true, file
        .getRepositoryItemUid());
    repo1.getLocalStorage().storeItem(repo1, link);

    StorageItem item = repo1.retrieveItem(new ResourceStoreRequest("/b.txt", true));
    assertEquals(DefaultStorageLinkItem.class, item.getClass());

    StorageFileItem item1 = (StorageFileItem) repo1.retrieveItem(false, new ResourceStoreRequest(
        ((StorageLinkItem) item).getTarget().getPath(),
        false));

    assertStorageFileItem(item1);
    assertTrue(contentEquals(item1.getInputStream(), new ByteArrayInputStream(contentString.getBytes())));
  }

}
