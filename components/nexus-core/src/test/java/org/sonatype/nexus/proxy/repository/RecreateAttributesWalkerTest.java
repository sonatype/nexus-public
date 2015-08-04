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
package org.sonatype.nexus.proxy.repository;

import java.util.Arrays;
import java.util.HashMap;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.M2TestsuiteEnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageCompositeFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.StorageCompositeFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RecreateAttributesWalkerTest
    extends AbstractProxyTestEnvironment
{
  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    return new M2TestsuiteEnvironmentBuilder(ss);
  }

  @Test
  public void testRecreateAttributes()
      throws Exception
  {
    // get a hosted repository
    final Repository repository = getRepositoryRegistry().getRepository("inhouse");

    // deploy some stuff in it
    // a file
    final ResourceStoreRequest fileRequest = new ResourceStoreRequest("/fileItem.txt");
    final StorageFileItem fileItem =
        new DefaultStorageFileItem(repository, fileRequest, true, true, new StringContentLocator(
            "This is a file."));
    repository.storeItem(false, fileItem);
    // a link
    final ResourceStoreRequest linkRequest = new ResourceStoreRequest("/linkItem.txt");
    final StorageLinkItem linkItem =
        new DefaultStorageLinkItem(repository, linkRequest, true, true, fileItem.getRepositoryItemUid());
    repository.storeItem(false, linkItem);
    // a composite
    final ResourceStoreRequest compositeRequest = new ResourceStoreRequest("/compositeItem.txt");
    final StorageCompositeFileItem compositeItem =
        new DefaultStorageCompositeFileItem(repository, compositeRequest, true, true, new StringContentLocator(
            "This is a Composite!"), Arrays.asList(new StorageItem[]{fileItem}));
    repository.storeItem(false, compositeItem);

    // recreate attributes
    final HashMap<String, String> initialData = new HashMap<String, String>();
    initialData.put("foo", "bar");
    final ResourceStoreRequest recreateAttributesRequest = new ResourceStoreRequest("/");
    repository.recreateAttributes(recreateAttributesRequest, initialData);

    // validate
    final StorageItem retrievedFileItem =
        repository.retrieveItem(false, new ResourceStoreRequest("/fileItem.txt"));
    final StorageItem retrievedLinkItem =
        repository.retrieveItem(false, new ResourceStoreRequest("/linkItem.txt"));
    final StorageItem retrievedCompositeItem =
        repository.retrieveItem(false, new ResourceStoreRequest("/compositeItem.txt"));

    // by presence of the "initial data" we validate that WalkerProcessor did process these types of items
    assertThat(retrievedFileItem.getRepositoryItemAttributes().containsKey("foo"), is(true));
    assertThat(retrievedLinkItem.getRepositoryItemAttributes().containsKey("foo"), is(true));
    assertThat(retrievedCompositeItem.getRepositoryItemAttributes().containsKey("foo"), is(true));
    assertThat(retrievedFileItem.getRepositoryItemAttributes().get("foo"), is("bar"));
    assertThat(retrievedLinkItem.getRepositoryItemAttributes().get("foo"), is("bar"));
    assertThat(retrievedCompositeItem.getRepositoryItemAttributes().get("foo"), is("bar"));
  }
}
