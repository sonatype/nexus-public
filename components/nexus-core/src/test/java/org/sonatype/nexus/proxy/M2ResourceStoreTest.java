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

import java.util.Collection;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Test;

public abstract class M2ResourceStoreTest
    extends AbstractProxyTestEnvironment
{

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);

    return new M2TestsuiteEnvironmentBuilder(ss);
  }

  protected abstract ResourceStore getResourceStore()
      throws NoSuchResourceStoreException, Exception;

  protected abstract String getItemPath();

  public void retrieveItem()
      throws Exception
  {
    StorageItem item = getResourceStore().retrieveItem(new ResourceStoreRequest(getItemPath(), false));
    checkForFileAndMatchContents(item);
  }

  @Test
  public void testRetrieveItem()
      throws Exception
  {
    retrieveItem();
  }

  @Test
  public void testCopyItem()
      throws Exception
  {
    retrieveItem();

    ResourceStoreRequest from = new ResourceStoreRequest(getItemPath(), true);

    ResourceStoreRequest to = new ResourceStoreRequest(getItemPath() + "-copy", true);

    getResourceStore().copyItem(from, to);

    StorageFileItem src = (StorageFileItem) getResourceStore().retrieveItem(
        new ResourceStoreRequest(getItemPath(), true));

    StorageFileItem dest = (StorageFileItem) getResourceStore().retrieveItem(
        new ResourceStoreRequest(getItemPath() + "-copy", true));

    checkForFileAndMatchContents(src, dest);
  }

  @Test
  public void testMoveItem()
      throws Exception
  {
    retrieveItem();

    ResourceStoreRequest from = new ResourceStoreRequest(getItemPath(), true);

    ResourceStoreRequest to = new ResourceStoreRequest(getItemPath() + "-copy", true);

    getResourceStore().moveItem(from, to);

    StorageItem item = getResourceStore().retrieveItem(new ResourceStoreRequest(getItemPath() + "-copy", true));

    checkForFileAndMatchContents(item, getRemoteFile(getRepositoryRegistry().getRepository(
        "repo1"), "/activemq/activemq-core/1.2/activemq-core-1.2.jar"));

    try {
      item = getResourceStore().retrieveItem(new ResourceStoreRequest(getItemPath(), true));

      fail();
    }
    catch (ItemNotFoundException e) {
      // good
    }
  }

  @Test
  public void testDeleteItem()
      throws Exception
  {
    retrieveItem();

    ResourceStoreRequest from = new ResourceStoreRequest(getItemPath(), true);

    getResourceStore().deleteItem(from);

    try {
      getResourceStore().retrieveItem(new ResourceStoreRequest(getItemPath(), true));

      fail();
    }
    catch (ItemNotFoundException e) {
      // good
    }
  }

  @Test
  public void testStoreItem()
      throws Exception
  {
    retrieveItem();

    StorageFileItem item = (StorageFileItem) getResourceStore().retrieveItem(
        new ResourceStoreRequest(getItemPath(), true));

    ResourceStoreRequest to = new ResourceStoreRequest(getItemPath() + "-copy", true);

    getResourceStore().storeItem(to, item.getInputStream(), null);

    StorageFileItem dest = (StorageFileItem) getResourceStore().retrieveItem(
        new ResourceStoreRequest(getItemPath() + "-copy", true));

    checkForFileAndMatchContents(dest, getRemoteFile(getRepositoryRegistry().getRepository(
        "repo1"), "/activemq/activemq-core/1.2/activemq-core-1.2.jar"));

  }

  @Test
  public void testCreateCollection()
      throws Exception
  {
    retrieveItem();

    ResourceStoreRequest req;
    if (Repository.class.isAssignableFrom(getResourceStore().getClass())) {
      req = new ResourceStoreRequest("/some/path", true);
    }
    else {
      req = new ResourceStoreRequest("/repo1/some/path", true);
    }

    getResourceStore().createCollection(req, null);

    assertTrue(getFile(getRepositoryRegistry().getRepository("repo1"), "/some/path").exists());
    assertTrue(getFile(getRepositoryRegistry().getRepository("repo1"), "/some/path").isDirectory());
  }

  @Test
  public void testList()
      throws Exception
  {
    retrieveItem();

    ResourceStoreRequest req = new ResourceStoreRequest("/", true);

    Collection<StorageItem> res = getResourceStore().list(req);

    for (StorageItem item : res) {
      System.out.println(item.getPath());
    }

    req = new ResourceStoreRequest("/", true);

    StorageCollectionItem coll = (StorageCollectionItem) getResourceStore().retrieveItem(req);

    for (StorageItem item : coll.list()) {
      System.out.println(item.getPath());
    }
  }

}
