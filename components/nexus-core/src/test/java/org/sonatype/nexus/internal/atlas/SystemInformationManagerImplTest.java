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
package org.sonatype.nexus.internal.atlas;

import java.util.Map;

import javax.cache.Cache;
import javax.cache.configuration.MutableConfiguration;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.atlas.SystemInformationGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class SystemInformationManagerImplTest
    extends TestSupport
{
  private static final Map<String, Object> SYSTEM_INFO_REPORT =
      ImmutableMap.of("systemInfo", ImmutableMap.of("nodeId", "testId"));

  private static final String CACHE_NAME = "SYSTEM_INFORMATION";

  private static final String SYSTEM_INFO_KEY = "systemInfo";

  @Mock
  private SystemInformationGenerator systemInformationGenerator;

  @Mock
  private CacheHelper cacheHelper;

  @Mock
  private Cache<String, Map<String, Object>> cache;

  private SystemInformationManagerImpl underTest;

  @Before
  public void setup() {
    underTest = new SystemInformationManagerImpl(systemInformationGenerator, cacheHelper, 300);

    when(systemInformationGenerator.report()).thenReturn(SYSTEM_INFO_REPORT);
    when(cacheHelper.maybeCreateCache(any(String.class), any(MutableConfiguration.class))).thenReturn(cache);

    underTest.doStart();
  }

  @After
  public void clear() {
    underTest.doStop();
  }

  @Test
  public void testGenerateAndCacheSystemInfoCorrectly() {
    when(cache.get(SYSTEM_INFO_KEY)).thenReturn(null);

    verify(cacheHelper, times(1)).maybeCreateCache(argThat(arg -> arg.equals(CACHE_NAME)),
        any(MutableConfiguration.class));

    Map<String, Object> systemInfo = underTest.getSystemInfo();
    verify(cache, times(1)).get(SYSTEM_INFO_KEY);
    verify(systemInformationGenerator, times(1)).report();
    verify(cache, times(1)).put(SYSTEM_INFO_KEY, SYSTEM_INFO_REPORT);
    assertThat(systemInfo, notNullValue());
    assertThat(systemInfo, is(SYSTEM_INFO_REPORT));
  }

  @Test
  public void testGetCachedSystemInfo() {
    when(cache.get(SYSTEM_INFO_KEY)).thenReturn(SYSTEM_INFO_REPORT);

    verify(cacheHelper, times(1)).maybeCreateCache(argThat(arg -> arg.equals(CACHE_NAME)),
        any(MutableConfiguration.class));

    Map<String, Object> systemInfo = underTest.getSystemInfo();
    verify(cache, times(1)).get(SYSTEM_INFO_KEY);
    verify(systemInformationGenerator, never()).report();
    verify(cache, never()).put(SYSTEM_INFO_KEY, SYSTEM_INFO_REPORT);
    assertThat(systemInfo, notNullValue());
    assertThat(systemInfo, is(SYSTEM_INFO_REPORT));
  }
}
