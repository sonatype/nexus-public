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
package org.apache.shiro.nexus;

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ShiroJCacheManagerAdapter}
 */
public class ShiroJCacheManagerAdapterTest
    extends TestSupport
{

  @Mock
  private CacheManager cacheManager;

  private ShiroJCacheManagerAdapter underTest;

  @Before
  public void setUp() {
    underTest = new ShiroJCacheManagerAdapter(() -> cacheManager);
    when(cacheManager.getCache(anyString())).thenReturn(null);
  }

  @Test
  public void defaultCacheConfigurationTest() throws Exception {
    ArgumentCaptor<Configuration> confCaptor = ArgumentCaptor.forClass(Configuration.class);
    when(cacheManager.createCache(anyString(), confCaptor.capture())).thenReturn(null);
    underTest.maybeCreateCache("foo");
    CompleteConfiguration configuration = (CompleteConfiguration)confCaptor.getValue();
    assertThat(configuration.isManagementEnabled(), is(true));
    assertThat(configuration.isStatisticsEnabled(), is(true));
    assertThat(configuration.getExpiryPolicyFactory(),
        is(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 2l)))
    );
  }

  @Test
  public void defaultShiroActiveSessionCacheConfigurationTest() throws Exception {
    ArgumentCaptor<Configuration> confCaptor = ArgumentCaptor.forClass(Configuration.class);
    when(cacheManager.createCache(anyString(), confCaptor.capture())).thenReturn(null);
    underTest.maybeCreateCache(CachingSessionDAO.ACTIVE_SESSION_CACHE_NAME);
    CompleteConfiguration configuration = (CompleteConfiguration)confCaptor.getValue();
    assertThat(configuration.isManagementEnabled(), is(true));
    assertThat(configuration.isStatisticsEnabled(), is(true));
    assertThat(configuration.getExpiryPolicyFactory(), is(EternalExpiryPolicy.factoryOf()));
  }

}
