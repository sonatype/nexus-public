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

import org.sonatype.goodies.testsupport.TestSupport;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Trials of basic EhCache.
 */
public class EhCacheTrial
    extends TestSupport
{
  private CacheManager underTest;

  @Before
  public void setUp() throws Exception {
    Configuration configuration = ConfigurationFactory.parseConfiguration(getClass().getResource("ehcache-test.xml"));
    configuration.setUpdateCheck(false);
    this.underTest = new CacheManager(configuration);
  }

  @After
  public void tearDown() throws Exception {
    if (underTest != null) {
      underTest.shutdown();
    }
  }

  @Test
  public void preconfiguredCacheWithEternal() {
    Cache cache = underTest.getCache("testEternalCache");
    log(cache);
    assertThat(cache, notNullValue());

    Object key = "foo";
    Object value = "bar";
    cache.put(new Element(key, value));

    Element result = cache.get(key);
    log(result);
    assertThat(result.getObjectValue(), is(value));

    result = cache.get(key);
    log(result);
    assertThat(result.getObjectValue(), is(value));

    result = cache.get(key);
    log(result);
    assertThat(result.getObjectValue(), is(value));
  }
}
