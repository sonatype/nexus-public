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
package org.sonatype.nexus.cache.internal.ehcache;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link EhCacheManagerProvider}.
 */
public class EhCacheManagerProviderTest
    extends TestSupport
{
  private EhCacheManagerProvider underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new EhCacheManagerProvider(getClass().getResource("ehcache-test.xml").toURI());
    underTest.start();
  }

  @After
  public void tearDown() throws Exception {
    // safety check that we always cleanup
    if (underTest != null) {
      underTest.stop();
    }
  }

  @Test
  public void basicLifecycle() throws Exception {
    // get should return non-null
    CacheManager cacheManager = underTest.get();
    assertThat(cacheManager, notNullValue());

    // repeated get should return same object
    CacheManager cacheManager2 = underTest.get();
    assertThat(cacheManager2, is(cacheManager));

    // after stop get should fail
    underTest.stop();
    try {
      underTest.get();
      fail();
    }
    catch (IllegalStateException e) {
      // expected
    }
    underTest.start();
  }

  @Test
  @Ignore("Disabled due to https://github.com/ehcache/ehcache-jcache/issues/40")
  public void preconfiguredCacheWithEternal() {
    CacheManager cacheManager = underTest.get();
    log(cacheManager);
    assertThat(cacheManager, notNullValue());

    Cache<Object,Object> cache = cacheManager.getCache("testEternalCache", Object.class, Object.class);
    log(cache);
    assertThat(cache, notNullValue());

    //log(JCacheConfiguration.class.getProtectionDomain().getCodeSource().getLocation());
    //JCacheConfiguration config = (JCacheConfiguration) cache.getConfiguration(JCacheConfiguration.class);
    //log(config.getExpiryPolicy().getExpiryForAccess());
    //log(config.getExpiryPolicy().getExpiryForAccess().isEternal());
    //log(config.getExpiryPolicy().getExpiryForAccess().isZero());

    Object key = "foo";
    Object value = "bar";
    Object result = cache.getAndPut(key, value);
    log(result);
    assertThat(result, nullValue());

    result = cache.get(key);
    log(result);
    assertThat(result, is(value));

    result = cache.get(key);
    log(result);
    assertThat(result, is(value));

    result = cache.get(key);
    log(result);
    assertThat(result, is(value));
  }
}
