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
package org.sonatype.nexus.testsuite.proxy

import java.util.concurrent.TimeUnit

import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CompleteConfiguration
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheKey
import org.sonatype.nexus.repository.view.Status

import org.ehcache.config.CacheRuntimeConfiguration
import org.ehcache.config.ResourceType
import org.ehcache.jsr107.Eh107Configuration

import static org.hamcrest.Matchers.is
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Ensure that cache configuration is as expected based on the defaults of the system.
 * @since 3.3
 */
class DefaultCacheSettingsTester
{
  final static long DEFAULT_HEAP_SIZE = 10000l

  /**
   * For 'negativeCache' backing NegativeCacheFacet activities, we expect specific ExpiryPolicy and heap size settings
   * for all Proxy repositories.
   */
  static void verifyNegativeCacheSettings(Repository repository, CacheManager cacheManager) {
    NestedAttributesMap cacheConfig = repository.configuration.attributes('negativeCache')
    boolean enabled = cacheConfig.get('enabled')
    if (enabled) {
      int ttl = cacheConfig.get('timeToLive')
      final Cache cache = cacheManager.
          getCache(repository.facet(NegativeCacheFacet).cacheName, NegativeCacheKey, Status)
      CompleteConfiguration<NegativeCacheKey, Status> completeConfiguration = cache.
          getConfiguration(CompleteConfiguration)

      assertThat(completeConfiguration.getExpiryPolicyFactory(),
          is(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, ttl))))
      verifyHeapSize(cache, DEFAULT_HEAP_SIZE)
    }
  }

  /**
   * Verify that the underlying cache configuration matches our expectations for heapSize.
   */
  static void verifyHeapSize(Cache cache, long heapSize) {
    Eh107Configuration eh107Configuration = cache.getConfiguration(Eh107Configuration)
    CacheRuntimeConfiguration runtimeConfiguration = eh107Configuration.unwrap(CacheRuntimeConfiguration)
    assertThat(runtimeConfiguration.resourcePools.getPoolForResource(ResourceType.Core.HEAP).size, is(heapSize))
  }

}
