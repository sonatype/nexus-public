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
package org.sonatype.nexus.plugins.lvo;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.plugins.lvo.config.LvoPluginConfiguration;
import org.sonatype.nexus.plugins.lvo.config.model.CLvoKey;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DefaultLvoServiceTest
    extends TestSupport
{
  @Mock
  private LvoPluginConfiguration cfg;

  private final Map<String, DiscoveryStrategy> strategies = Maps.newHashMap();

  private DefaultLvoService underTest;

  @Before
  public void setUp() {
    when(cfg.isEnabled()).thenReturn(true);
    underTest = new DefaultLvoService(cfg, strategies);
  }

  @Test
  public void testGetVersionTouchesStrategy()
      throws NoSuchStrategyException, IOException, NoSuchKeyException, NoSuchRepositoryException
  {
    final Function<DiscoveryResponse, DiscoveryResponse> func =
        new Function<DiscoveryResponse, DiscoveryResponse>()
        {
          @Override
          public DiscoveryResponse apply(@Nullable final DiscoveryResponse input) {
            return input;
          }
        };

    final CLvoKey key = mockOut(func);

    DiscoveryResponse response = underTest.getLatestVersionForKey("test");
    DiscoveryRequest request = response.getRequest();
    assertThat(request.getKey(), is("test"));
    assertThat(request.getLvoKey(), is(key));

    verify(strategies.get("test")).discoverLatestVersion(request);
  }

  private CLvoKey mockOut(final Function<DiscoveryResponse, DiscoveryResponse> func)
      throws NoSuchKeyException, NoSuchRepositoryException, IOException
  {
    final CLvoKey key = mock(CLvoKey.class);
    DiscoveryStrategy strategy = mock(DiscoveryStrategy.class);
    strategies.put("test", strategy);

    when(cfg.getLvoKey("test")).thenReturn(key);
    when(key.getStrategy()).thenReturn("test");
    when(strategy.discoverLatestVersion(any(DiscoveryRequest.class))).then(new Answer<Object>()
    {
      @Override
      public Object answer(final InvocationOnMock invocationOnMock)
          throws Throwable
      {
        DiscoveryRequest request = (DiscoveryRequest) invocationOnMock.getArguments()[0];
        return func.apply(new DiscoveryResponse(request));
      }
    });
    return key;
  }

  @Test
  public void testQueryVersionNotNewer()
      throws NoSuchStrategyException, IOException, NoSuchKeyException, NoSuchRepositoryException
  {
    mockOut(new Function<DiscoveryResponse, DiscoveryResponse>()
    {
      @Override
      public DiscoveryResponse apply(@Nullable final DiscoveryResponse input) {
        input.setSuccessful(true);
        input.setVersion("1");
        input.setUrl("http://some.url");
        return spy(input);
      }
    });

    DiscoveryResponse response = underTest.queryLatestVersionForKey("test", "2");
    verify(response, atLeastOnce()).isSuccessful();
    verify(response).getVersion();
    verify(response).setSuccessful(false);

    assertThat(response.isSuccessful(), is(false));
  }

  @Test
  public void testQueryVersionNewer()
      throws NoSuchStrategyException, IOException, NoSuchKeyException, NoSuchRepositoryException
  {
    mockOut(new Function<DiscoveryResponse, DiscoveryResponse>()
    {
      @Override
      public DiscoveryResponse apply(@Nullable final DiscoveryResponse input) {
        input.setSuccessful(true);
        input.setVersion("3");
        input.setUrl("http://some.url");
        return spy(input);
      }
    });

    DiscoveryResponse response = underTest.queryLatestVersionForKey("test", "2");
    verify(response, atLeastOnce()).isSuccessful();
    verify(response).getVersion();

    assertThat(response.isSuccessful(), is(true));
  }

}
