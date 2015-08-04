/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.proxy;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.MetadataServiceFactory;
import com.bolyuba.nexus.plugin.npm.service.ProxyMetadataService;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballSource;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultNpmProxyRepositoryTest
    extends TestSupport
{
  private DefaultNpmProxyRepository proxy;

  @Before
  public void prepare() {
    // dummy uid and uidLock
    final RepositoryItemUid uid = mock(RepositoryItemUid.class);
    when(uid.getLock()).thenReturn(mock(RepositoryItemUidLock.class));
    // not using mock as it would OOM when it tracks invocations, as we work with large files here
    final MetadataServiceFactory metadataServiceFactory = mock(MetadataServiceFactory.class);
    when(metadataServiceFactory.createProxyMetadataService(any(NpmProxyRepository.class)))
        .thenReturn(mock(ProxyMetadataService.class));
    proxy = new DefaultNpmProxyRepository(mock(ContentClass.class), mock(
        NpmProxyRepositoryConfigurator.class), metadataServiceFactory, mock(TarballSource.class))
    {
      @Override
      public String getId() {
        return "proxy";
      }

      @Override
      public boolean isItemAgingActive() { return true; }

      @Override
      public int getItemMaxAge() { return 10; }

      @Override
      public String getRemoteUrl() { return "http://registry.npmjs.org/"; }

      @Override
      protected boolean shouldServiceOperation(final ResourceStoreRequest request, final String action) {
        return true;
      }

      @Override
      protected EventBus eventBus() {
        return mock(EventBus.class);
      }

      @Override
      public RepositoryItemUid createUid(String path) { return uid; }
    };
  }

  protected DefaultStorageFileItem createNpmMetadataItem() {
    return proxy.createStorageFileItem(new ResourceStoreRequest("/package"), new StringContentLocator("unimportant"));
  }

  @Test
  public void metadataItemIsNeverOld() {
    final DefaultStorageFileItem item = createNpmMetadataItem();
    // maxAge=0 means "always old"
    assertThat(proxy.isOld(0, item, true), is(false));
  }

  @Test
  public void metadataItemIsNeverOldEvenIfExpired() {
    final DefaultStorageFileItem item = createNpmMetadataItem();
    // expired = true
    item.setExpired(true);
    assertThat(proxy.isOld(0, item, true), is(false));
  }

  @Test
  public void metadataItemIsNeverOldEvenIfRepoExpired() {
    proxy.expireProxyCaches(new ResourceStoreRequest("/"));
    final DefaultStorageFileItem item = createNpmMetadataItem();
    // repo has been expired, so token is set
    assertThat(proxy.isOld(0, item, true), is(false));
  }
}
