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
package org.sonatype.nexus.repository.cache.internal;

import java.util.Arrays;
import java.util.Iterator;
import javax.cache.Cache;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.NegativeCacheKey;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.view.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NegativeCacheFacetImplTest
    extends TestSupport
{
  @Mock
  private NegativeCacheKey key;

  @Mock
  private CacheHelper cacheHelper;

  @Mock
  private Cache cache;

  @Mock
  private Repository repository;

  private NegativeCacheFacetImpl.Config config;

  private Status status;

  private NegativeCacheFacetImpl underTest;

  @Before
  public void setUp() {
    cacheHelper = mock(CacheHelper.class);
    cache = mock(Cache.class);
    when(cacheHelper.maybeCreateCache(any(), any(), any(), any())).thenReturn(cache);
    underTest = new NegativeCacheFacetImpl(cacheHelper);
    underTest.installDependencies(mock(EventManager.class));
    key = mock(NegativeCacheKey.class);
    status = Status.failure(HttpStatus.NOT_FOUND, "404");
    repository = mock(Repository.class);
    when(repository.getName()).thenReturn("test");

    config = new NegativeCacheFacetImpl.Config();
    config.enabled = false;
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    when(configurationFacet.readSection(
        any(),
        eq(NegativeCacheFacetImpl.CONFIG_KEY),
        eq(NegativeCacheFacetImpl.Config.class)))
            .thenReturn(config);
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
  }

  @Test
  public void noConfigurationPresentNoCache() throws Exception {
    config = null;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    verify(cacheHelper, never()).maybeCreateCache(any(String.class), any(Class.class), any(Class.class), any());
    assertThat(underTest.get(key), nullValue());
    underTest.put(key, Status.failure(HttpStatus.NOT_FOUND, "404"));
    underTest.invalidate(key);
    underTest.invalidate();
    underTest.stop();
    underTest.destroy();
    verify(cacheHelper, never()).maybeDestroyCache(any(String.class));
  }

  @Test
  public void notEnabledNoCache() throws Exception {
    config.enabled = false;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    verify(cacheHelper, never()).maybeCreateCache(any(String.class), any(Class.class), any(Class.class), any());
    assertThat(underTest.get(key), nullValue());
    underTest.put(key, Status.failure(HttpStatus.NOT_FOUND, "404"));
    underTest.invalidate(key);
    underTest.invalidate();
    underTest.stop();
    underTest.destroy();
    verify(cacheHelper, never()).maybeDestroyCache(any(String.class));
  }

  @Test
  public void cacheIsCreatedAndRemoved() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    ArgumentCaptor<String> cacheNameCaptor = ArgumentCaptor.forClass(String.class);
    verify(cacheHelper).maybeCreateCache(cacheNameCaptor.capture(), any(Class.class), any(Class.class), any());
    when(cache.getName()).thenReturn(cacheNameCaptor.getValue());
    underTest.stop();
    underTest.destroy();
    verify(cacheHelper, never()).maybeDestroyCache(any(String.class));
  }

  @Test
  public void cacheIsNotRemovedWhenManagerIsNotActive() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    underTest.stop();
    underTest.destroy();
    verify(cacheHelper, never()).maybeDestroyCache(any(String.class));
  }

  @Test
  public void putCachesElement() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    underTest.put(key, status);
    ArgumentCaptor<NegativeCacheKey> keyCaptor = ArgumentCaptor.forClass(NegativeCacheKey.class);
    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    verify(cache).put(keyCaptor.capture(), statusCaptor.capture());
    assertThat(keyCaptor.getValue(), equalTo(key));
    assertThat(statusCaptor.getValue(), equalTo(status));
  }

  @Test
  public void getReturnsStatus() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    when(cache.get(key)).thenReturn(status);
    Status actualStatus = underTest.get(key);
    assertThat(actualStatus, equalTo(status));
  }

  @Test
  public void getReturnsNullWhenCacheReturnsNull() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    when(cache.get(key)).thenReturn(null);
    Status actualStatus = underTest.get(key);
    assertThat(actualStatus, nullValue());
  }

  @Test
  public void invalidateRemovesElement() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    underTest.invalidate(key);
    verify(cache).remove(key);
  }

  @Test
  public void invalidateRemovesAllElements() throws Exception {
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    underTest.invalidate();
    verify(cache).removeAll();
  }

  @Test
  public void invalidateSubsetRemovesKeyAndAllChildKeys() throws Exception {
    NegativeCacheKey key1 = mock(NegativeCacheKey.class);
    NegativeCacheKey key2 = mock(NegativeCacheKey.class);
    Cache.Entry<NegativeCacheKey, Status> entry1 = mock(Cache.Entry.class);
    Cache.Entry<NegativeCacheKey, Status> entry2 = mock(Cache.Entry.class);
    when(entry1.getKey()).thenReturn(key1);
    when(entry2.getKey()).thenReturn(key2);
    mockIterable(cache, entry1, entry2);
    when(key.isParentOf(key1)).thenReturn(false);
    when(key.isParentOf(key2)).thenReturn(true);
    config.enabled = true;
    underTest.attach(repository);
    underTest.init();
    underTest.start();
    underTest.invalidateSubset(key);
    verify(cache).remove(key);
    verify(cache, never()).remove(key1);
    verify(cache).remove(key2);
  }

  private static void mockIterable(
      Cache<NegativeCacheKey, Status> iterable,
      Cache.Entry<NegativeCacheKey, Status>... values)
  {
    Iterator<Cache.Entry<NegativeCacheKey, Status>> mockIterator = mock(Iterator.class);
    when(iterable.iterator()).thenReturn(mockIterator);

    if (values.length == 0) {
      when(mockIterator.hasNext()).thenReturn(false);
    }
    else if (values.length == 1) {
      when(mockIterator.hasNext()).thenReturn(true, false);
      when(mockIterator.next()).thenReturn(values[0]);
    }
    else {
      Boolean[] hasNextResponses = new Boolean[values.length];
      for (int i = 0; i < hasNextResponses.length - 1; i++) {
        hasNextResponses[i] = true;
      }
      hasNextResponses[hasNextResponses.length - 1] = false;
      when(mockIterator.hasNext()).thenReturn(true, hasNextResponses);
      Cache.Entry<NegativeCacheKey, Status>[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length);
      when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst);
    }
  }
}
