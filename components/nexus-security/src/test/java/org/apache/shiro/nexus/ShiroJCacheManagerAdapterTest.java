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

import javax.cache.configuration.Factory;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cache.CacheHelper;

import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ShiroJCacheManagerAdapter}
 */
public class ShiroJCacheManagerAdapterTest
    extends TestSupport
{

  @Mock
  private CacheHelper cacheHelper;

  @Captor
  private ArgumentCaptor<Factory<ExpiryPolicy>> confCaptor;

  private ShiroJCacheManagerAdapter underTest;

  @Before
  public void setUp() {
    underTest = new ShiroJCacheManagerAdapter(() -> cacheHelper, () -> Time.minutes(2L));
  }

  @Test
  public void defaultCacheConfigurationTest() throws Exception {
    when(cacheHelper.maybeCreateCache(anyString(), confCaptor.capture())).thenReturn(null);
    underTest.maybeCreateCache("foo");
    assertThat(confCaptor.getValue(), is(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 2L))));
  }

  @Test
  public void defaultShiroActiveSessionCacheConfigurationTest() throws Exception {
    when(cacheHelper.maybeCreateCache(anyString(), confCaptor.capture())).thenReturn(null);
    underTest.maybeCreateCache(CachingSessionDAO.ACTIVE_SESSION_CACHE_NAME);
    assertThat(confCaptor.getValue(), is(EternalExpiryPolicy.factoryOf()));
  }

}
