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
package org.sonatype.nexus.repository.cache.internal

import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.MutableConfiguration

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventBus
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.cache.NegativeCacheKey
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.config.ConfigurationFacet
import org.sonatype.nexus.repository.http.HttpStatus
import org.sonatype.nexus.repository.view.Status

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor

import static org.mockito.Matchers.any
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Tests for {@link NegativeCacheHandler}.
 */
class NegativeCacheFacetImplTest
    extends TestSupport
{
  private NegativeCacheFacetImpl underTest

  private NegativeCacheKey key

  private Status status

  private CacheManager cacheManager

  private Cache cache

  private Repository repository

  private NegativeCacheFacetImpl.Config config

  @Before
  void setUp() {
    cacheManager = mock(CacheManager)
    when(cacheManager.isClosed()).thenReturn(false)
    cache = mock(Cache)
    when(cacheManager.createCache(any(String), any(MutableConfiguration))).thenReturn(cache)
    underTest = new NegativeCacheFacetImpl(cacheManager)
    underTest.installDependencies(mock(EventBus))
    key = mock(NegativeCacheKey)
    status = Status.failure(HttpStatus.NOT_FOUND, '404')
    repository = mock(Repository)
    when(repository.name).thenReturn('test')

    config = new NegativeCacheFacetImpl.Config()
    config.enabled = false
    def configurationFacet = mock(ConfigurationFacet.class)
    when(configurationFacet.readSection(
        any(Configuration.class),
        eq(NegativeCacheFacetImpl.CONFIG_KEY),
        eq(NegativeCacheFacetImpl.Config.class)))
        .thenReturn(config)
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet)
  }

  /**
   * Given:
   * - no configuration present
   * Then:
   * - cache is not created
   * - get returns null
   * - facet methods skip cache invocations
   * - destroy does not remove cache
   */
  @Test
  void 'no configuration present no cache'() {
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    verify(cacheManager, never()).createCache(any(String), any(MutableConfiguration))
    assert underTest.get(key) == null
    underTest.put(key, Status.failure(HttpStatus.NOT_FOUND, '404'))
    underTest.invalidate(key)
    underTest.invalidate()
    underTest.stop()
    underTest.destroy()
    verify(cacheManager, never()).destroyCache(any(String))
  }

  /**
   * Given:
   * - configuration present
   * - enabled = false
   * Then:
   * - cache is not created
   * - get returns null
   * - facet methods skip cache invocations
   * - cache is not removed
   */
  @Test
  void 'not enabled no cache'() {
    config.enabled = false
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    verify(cacheManager, never()).createCache(any(String), any(MutableConfiguration))
    assert underTest.get(key) == null
    underTest.put(key, Status.failure(HttpStatus.NOT_FOUND, '404'))
    underTest.invalidate(key)
    underTest.invalidate()
    underTest.stop()
    underTest.destroy()
    verify(cacheManager, never()).destroyCache(any(String))
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * Then:
   * - cache is created
   * - cache is removed
   */
  @Test
  void 'cache is created and removed'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    ArgumentCaptor<String> cacheNameCaptor = ArgumentCaptor.forClass(String)
    ArgumentCaptor<MutableConfiguration> configCaptor = ArgumentCaptor.forClass(MutableConfiguration)
    verify(cacheManager).createCache(cacheNameCaptor.capture(), configCaptor.capture())
    when(cache.name).thenReturn(cacheNameCaptor.value)
    underTest.stop()
    underTest.destroy()
    verify(cacheManager).destroyCache(cacheNameCaptor.value)
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * - cache manager is not active (already being shutdown)
   * Then:
   * - cache is not removed
   */
  @Test
  void 'cache is not removed when manager is not active'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    when(cacheManager.isClosed()).thenReturn(true)
    underTest.stop()
    underTest.destroy()
    verify(cacheManager, never()).destroyCache(any(String))
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * Then:
   * - put puts key/status into cache
   */
  @Test
  void 'put caches element'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    underTest.put(key, status)
    ArgumentCaptor<NegativeCacheKey> keyCaptor = ArgumentCaptor.forClass(NegativeCacheKey);
    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status);
    verify(cache).put(keyCaptor.capture(), statusCaptor.capture())
    assert key == keyCaptor.value
    assert status == statusCaptor.value
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * - cache returns an element
   * Then:
   * - get retrieves element from cache and returns status
   */
  @Test
  void 'get returns status'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    when(cache.get(key)).thenReturn(status)
    Status actualStatus = underTest.get(key)
    assert actualStatus == status
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * - cache does not have an element for key (returns null)
   * Then:
   * - get retrieves element from cache and returns null
   */
  @Test
  void 'get returns null when cache returns null'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    when(cache.get(key)).thenReturn(null)
    Status actualStatus = underTest.get(key)
    assert actualStatus == null
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * Then:
   * - invalidate removes key from cache
   */
  @Test
  void 'invalidate removes element'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    underTest.invalidate(key)
    verify(cache).remove(key)
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * Then:
   * - invalidate removes all keys from cache
   */
  @Test
  void 'invalidate removes all elements'() {
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    underTest.invalidate()
    verify(cache).removeAll()
  }

  /**
   * Given:
   * - configuration present
   * - enabled = true
   * - cached entries
   * Then:
   * - invalidate removes all child keys from cache
   */
  @Test
  void 'invalidate subset removes key and all child keys'() {
    NegativeCacheKey key1 = mock(NegativeCacheKey)
    NegativeCacheKey key2 = mock(NegativeCacheKey)
    Cache.Entry<NegativeCacheKey, Status> entry1 = mock(Cache.Entry)
    Cache.Entry<NegativeCacheKey, Status> entry2 = mock(Cache.Entry)
    when(entry1.key).thenReturn(key1)
    when(entry2.key).thenReturn(key2)
    mockIterable(cache, entry1, entry2)
    when(key.isParentOf(key1)).thenReturn(false)
    when(key.isParentOf(key2)).thenReturn(true)
    config.enabled = true
    underTest.attach(repository)
    underTest.init()
    underTest.start()
    underTest.invalidateSubset(key)
    verify(cache).remove(key)
    verify(cache, never()).remove(key1)
    verify(cache).remove(key2)
  }

  static void mockIterable(Cache<?,?> iterable, Object... values) {
    Iterator<?> mockIterator = mock(Iterator)
    when(iterable.iterator()).thenReturn(mockIterator)

    if (values.length == 0) {
      when(mockIterator.hasNext()).thenReturn(false)
    }
    else if (values.length == 1) {
      when(mockIterator.hasNext()).thenReturn(true, false)
      when(mockIterator.next()).thenReturn(values[0])
    }
    else {
      Boolean[] hasNextResponses = new Boolean[values.length]
      for (int i = 0; i < hasNextResponses.length-1; i++) {
        hasNextResponses[i] = true
      }
      hasNextResponses[hasNextResponses.length - 1] = false
      when(mockIterator.hasNext()).thenReturn(true, hasNextResponses)
      Object[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length)
      when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst)
    }
  }
}
