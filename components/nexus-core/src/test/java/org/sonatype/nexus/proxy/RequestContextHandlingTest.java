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
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * UT that verifies is request context properly passed over to affected storage items.
 */
public class RequestContextHandlingTest
    extends AbstractProxyTestEnvironment
{
  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  @Override
  protected synchronized EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    if (jettyTestsuiteEnvironmentBuilder == null) {
      ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
      this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    }
    return jettyTestsuiteEnvironmentBuilder;

  }

  @Before
  public void prepare() throws Exception {
    final String contentString = "SOME_CONTENT";
    Repository inhouse = getRepositoryRegistry().getRepository("inhouse");
    DefaultStorageFileItem file = new DefaultStorageFileItem(
        inhouse,
        new ResourceStoreRequest("/a.txt"),
        true,
        true,
        new StringContentLocator(contentString));
    inhouse.storeItem(false, file);
    DefaultStorageLinkItem link = new DefaultStorageLinkItem(inhouse, new ResourceStoreRequest("/b.txt"), true, true,
        file.getRepositoryItemUid());
    inhouse.storeItem(false, link);
  }

  final String MARKER_KEY = RequestContextHandlingTest.class.getName() + ".marker";

  final String MARKER_VALUE = "marker";

  /**
   * Creates a request that in context has marker set.
   */
  protected ResourceStoreRequest createRequest(final String path) {
    final ResourceStoreRequest result = new ResourceStoreRequest(path);
    // to not have security meddle
    result.getRequestContext().put(AccessManager.REQUEST_AUTHORIZED, Boolean.TRUE);
    // to have a marker in context
    result.getRequestContext().put(MARKER_KEY, MARKER_VALUE);
    return result;
  }

  /**
   * Note: all tests are in single method, as these basically have no side effect, so to avoid the penality of
   * starting-stopping Nexus at every UT method.
   */
  @Test
  public void contextHandling()
      throws Exception
  {
    // direct repo access
    {
      final ResourceStoreRequest request = createRequest("/repositories/inhouse/a.txt");
      final StorageItem item = getRootRouter().retrieveItem(request);
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
    }

    {
      final ResourceStoreRequest request = createRequest("/repositories/inhouse/b.txt");
      // b.txt is a LINK, and root router will auto-resolve it to a.txt
      final StorageItem item = getRootRouter().retrieveItem(request);
      // dereferencing did occur (note: we asked for b.txt!)
      assertThat(item.getRepositoryItemUid().getPath(), equalTo("/a.txt"));
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
    }

    {
      final ResourceStoreRequest request = createRequest("/repositories/inhouse/");
      final StorageItem item = getRootRouter().retrieveItem(request);
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
      // is a collection
      assertThat(item, instanceOf(StorageCollectionItem.class));
      final StorageCollectionItem root = (StorageCollectionItem) item;
      final Collection<StorageItem> children = root.list();
      for (StorageItem child : children) {
        // marker arrived back in item context even for children
        assertThat(child.getItemContext().flatten(), hasKey(MARKER_KEY));
      }
    }

    // group repo access
    {
      final ResourceStoreRequest request = createRequest("/repositories/test/a.txt");
      final StorageItem item = getRootRouter().retrieveItem(request);
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
    }

    {
      final ResourceStoreRequest request = createRequest("/repositories/test/b.txt");
      // b.txt is a LINK, and root router will auto-resolve it to a.txt
      final StorageItem item = getRootRouter().retrieveItem(request);
      // dereferencing did occur (note: we asked for b.txt!)
      assertThat(item.getRepositoryItemUid().getPath(), equalTo("/a.txt"));
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
    }

    {
      final ResourceStoreRequest request = createRequest("/repositories/test/");
      final StorageItem item = getRootRouter().retrieveItem(request);
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
      // is a collection
      assertThat(item, instanceOf(StorageCollectionItem.class));
      final StorageCollectionItem root = (StorageCollectionItem) item;
      final Collection<StorageItem> children = root.list();
      for (StorageItem child : children) {
        // marker arrived back in item context even for children
        assertThat(child.getItemContext().flatten(), hasKey(MARKER_KEY));
      }
    }

    // virtual items
    {
      final ResourceStoreRequest request = createRequest("/repositories/");
      final StorageItem item = getRootRouter().retrieveItem(request);
      // marker arrived back in item context
      assertThat(item.getItemContext().flatten(), hasKey(MARKER_KEY));
      // is a collection
      assertThat(item, instanceOf(StorageCollectionItem.class));
      final StorageCollectionItem root = (StorageCollectionItem) item;
      final Collection<StorageItem> children = root.list();
      for (StorageItem child : children) {
        // marker arrived back in item context even for children
        assertThat(child.getItemContext().flatten(), hasKey(MARKER_KEY));
      }
    }
  }

}
