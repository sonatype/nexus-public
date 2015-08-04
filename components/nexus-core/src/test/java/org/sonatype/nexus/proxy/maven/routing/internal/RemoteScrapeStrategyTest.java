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
package org.sonatype.nexus.proxy.maven.routing.internal;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyFailedException;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;

public class RemoteScrapeStrategyTest
    extends NexusAppTestSupport
{
  @Mock
  private MavenProxyRepository mavenProxyRepository;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    when(mavenProxyRepository.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));
  }

  @Test(expected = StrategyFailedException.class)
  public void checkNoFtpIsScraped()
      throws Exception
  {
    when(mavenProxyRepository.getRemoteUrl()).thenReturn("ftp://host.com/repo/");
    final RemoteScrapeStrategy rss = (RemoteScrapeStrategy) lookup(RemoteStrategy.class, RemoteScrapeStrategy.ID);
    rss.discover(mavenProxyRepository);
  }

  @Test(expected = StrategyFailedException.class)
  public void checkOnlyHttpIsScraped()
      throws Exception
  {
    when(mavenProxyRepository.getRemoteUrl()).thenReturn("groups:thegroup/");
    final RemoteScrapeStrategy rss = (RemoteScrapeStrategy) lookup(RemoteStrategy.class, RemoteScrapeStrategy.ID);
    rss.discover(mavenProxyRepository);
  }
}
