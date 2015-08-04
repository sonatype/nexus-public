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
package org.sonatype.nexus.proxy.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

public class DefaultStorageCollectionItemTest
    extends AbstractStorageItemTest
{
  @Test
  public void testNonVirtualCollectionSimple() {

    doReturn("dummy").when(repository).getId();
    doAnswer(new Answer<RepositoryItemUid>()
    {
      public RepositoryItemUid answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        return getRepositoryItemUidFactory().createUid(repository, (String) args[0]);
      }
    }).when(repository).createUid("/");

    DefaultStorageCollectionItem coll = new DefaultStorageCollectionItem(repository, new ResourceStoreRequest("/"), true, true);
    checkAbstractStorageItem(repository, coll, false, "", "/", "/");
  }

  @Test
  public void testNonVirtualCollectionList()
      throws Exception
  {
    List<StorageItem> result = new ArrayList<StorageItem>();

    doReturn("dummy").when(repository).getId();
    doAnswer(new Answer<RepositoryItemUid>()
    {
      public RepositoryItemUid answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        return getRepositoryItemUidFactory().createUid(repository, (String) args[0]);
      }
    }).when(repository).createUid("/a/some/dir/coll");
    String[] items = new String[]{"A", "B", "C"};
    for (String item : items) {
      doAnswer(new Answer<RepositoryItemUid>()
      {
        public RepositoryItemUid answer(InvocationOnMock invocation) {
          Object[] args = invocation.getArguments();
          return getRepositoryItemUidFactory().createUid(repository, (String) args[0]);
        }
      }).when(repository).createUid("/a/some/dir/coll/" + item);
    }
    doReturn(result).when(repository).list(Matchers.anyBoolean(), Matchers.isA(StorageCollectionItem.class));

    // and now fill in result, since repo is active
    result.add(new DefaultStorageFileItem(repository, new ResourceStoreRequest("/a/some/dir/coll/A"), true, true, new StringContentLocator(
        "A")));
    result.add(new DefaultStorageFileItem(repository, new ResourceStoreRequest("/a/some/dir/coll/B"), true, true, new StringContentLocator(
        "B")));
    result.add(new DefaultStorageFileItem(repository, new ResourceStoreRequest("/a/some/dir/coll/C"), true, true, new StringContentLocator(
        "C")));

    DefaultStorageCollectionItem coll =
        new DefaultStorageCollectionItem(repository, new ResourceStoreRequest("/a/some/dir/coll"), true, true);
    checkAbstractStorageItem(repository, coll, false, "coll", "/a/some/dir/coll", "/a/some/dir");

    Collection<StorageItem> resultItems = coll.list();
    assertEquals(3, resultItems.size());
  }

  @Test
  public void testVirtualCollectionSimple() {
    DefaultStorageCollectionItem coll = new DefaultStorageCollectionItem(router, new ResourceStoreRequest("/"), true, true);
    checkAbstractStorageItem(router, coll, true, "", "/", "/");
  }

  @Test
  public void testVirtualCollectionList()
      throws Exception
  {
    List<StorageItem> result = new ArrayList<StorageItem>();
    doReturn(result).when(router).list(Matchers.isA(ResourceStoreRequest.class));

    // and now fill in result, since repo is active
    result.add(new DefaultStorageFileItem(router, new ResourceStoreRequest("/a/some/dir/coll/A"), true, true,
        new StringContentLocator("A")));
    result.add(new DefaultStorageFileItem(router, new ResourceStoreRequest("/a/some/dir/coll/B"), true, true,
        new StringContentLocator("B")));

    DefaultStorageCollectionItem coll = new DefaultStorageCollectionItem(router, new ResourceStoreRequest("/and/another/coll"), true, true);
    checkAbstractStorageItem(router, coll, true, "coll", "/and/another/coll", "/and/another");

    Collection<StorageItem> items = coll.list();
    assertEquals(2, items.size());
  }

}
