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
package org.sonatype.nexus.proxy.cache;

import java.util.List;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EhCachePathCache}.
 */
@RunWith(MockitoJUnitRunner.class)
public class EhCachePathCacheTest
{

  @Captor
  ArgumentCaptor<Element> elementCaptor;

  @Mock
  Ehcache ehcache;

  /**
   * Related to NEXUS-5166, since ehcache 2.5
   */
  @Test
  public void doPutOnlySetsTimeToLiveOnElementsWhenExpirationGreaterThanNegOne() {

    EhCachePathCache cache = spy(new EhCachePathCache("fake", ehcache));
    when(cache.getEHCache()).thenReturn(ehcache);

    cache.put("path1", new Object(), -2);
    cache.put("path2", new Object(), -1);
    cache.put("path3", new Object(), 0);
    cache.put("path4", new Object(), 1);
    cache.put("path5", new Object(), 10000);

    verify(cache, times(5)).getEHCache();
    verify(ehcache, times(5)).put(elementCaptor.capture());

    List<Element> elements = elementCaptor.getAllValues();
    assertThat(elements.get(0).getTimeToLive(), equalTo(0));
    assertThat(elements.get(1).getTimeToLive(), equalTo(0));
    assertThat(elements.get(2).getTimeToLive(), equalTo(0));
    assertThat(elements.get(3).getTimeToLive(), equalTo(1));
    assertThat(elements.get(4).getTimeToLive(), equalTo(10000));

  }
}
