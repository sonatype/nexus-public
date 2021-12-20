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
package org.sonatype.nexus.plugins.lvo.strategy;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.apachehttpclient.Hc4ProviderImpl;
import org.sonatype.nexus.apachehttpclient.PoolingClientConnectionManagerMBeanInstaller;
import org.sonatype.nexus.configuration.application.DefaultNexusConfiguration;
import org.sonatype.nexus.plugins.lvo.DiscoveryRequest;
import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.goodies.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Proxy;
import org.sonatype.tests.http.server.fluent.Server;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.sonatype.tests.http.server.fluent.Behaviours.content;
import static org.sonatype.tests.http.server.fluent.Behaviours.error;
import static org.sonatype.tests.http.server.fluent.Behaviours.redirect;

/**
 * Tests for network parts of AbstractRemoteDiscoveryStrategy
 */
public class AbstractRemoteDiscoveryStrategyTest
    extends TestSupport
{

  private Server server;

  private String content;

  private AbstractRemoteDiscoveryStrategy.RequestResult result;

  @Mock
  private DefaultNexusConfiguration cfg;

  @Mock
  private RemoteStorageContext remoteStorageContext;

  @Mock
  private RemoteProxySettings remoteProxySettings;

  @Mock
  private UserAgentBuilder userAgentBuilder;

  @Mock
  private EventBus eventBus;

  @Mock
  private PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

  @Before
  public void setup()
      throws Exception
  {
    content = "nexus-oss.version=2.0\nnexus-oss.url=http://some.url\n";

    when(cfg.getGlobalRemoteStorageContext()).thenReturn(remoteStorageContext);
    when(remoteStorageContext.getRemoteProxySettings()).thenReturn(remoteProxySettings);
    when(remoteStorageContext.getRemoteConnectionSettings()).thenReturn(new DefaultRemoteConnectionSettings());
  }

  @After
  public void cleanup()
      throws Exception
  {
    if (server != null) {
      server.stop();
    }

    if (result != null) {
      result.close();
    }
  }

  private AbstractRemoteDiscoveryStrategy create() {
    final Hc4Provider provider = new Hc4ProviderImpl(cfg, userAgentBuilder, eventBus, jmxInstaller, null);
    return new AbstractRemoteDiscoveryStrategy(provider)
    {
      @Override
      public DiscoveryResponse discoverLatestVersion(final DiscoveryRequest request)
          throws NoSuchRepositoryException, IOException
      {
        return null;
      }
    };
  }

  @Test
  public void testDirect()
      throws Exception
  {
    server = Server.withPort(0).serve("/test.properties").withBehaviours(content(content)).start();

    AbstractRemoteDiscoveryStrategy underTest = create();

    AbstractRemoteDiscoveryStrategy.RequestResult result =
        underTest.handleRequest(server.getUrl().toString() + "/test.properties");

    assertThat(result.getInputStream(), notNullValue());
    assertThat("content did not match",
        IOUtils.contentEquals(result.getInputStream(), new ByteArrayInputStream(content.getBytes())), is(true));
  }

  @Test
  public void testProxy()
      throws Exception
  {
    server = Proxy.withPort(0).serve("/test.properties").withBehaviours(content(content)).start();

    final DefaultRemoteHttpProxySettings httpProxySettings = new DefaultRemoteHttpProxySettings();
    httpProxySettings.setHostname("localhost");
    httpProxySettings.setPort(server.getPort());

    when(remoteProxySettings.getHttpProxySettings()).thenReturn(httpProxySettings);

    AbstractRemoteDiscoveryStrategy underTest = create();

    result = underTest.handleRequest("http://invalid.url/test.properties");

    assertThat(result.getInputStream(), notNullValue());
    assertThat("content did not match",
        IOUtils.contentEquals(result.getInputStream(), new ByteArrayInputStream(content.getBytes())), is(true));
  }

  @Test
  public void testDirectFails()
      throws Exception
  {
    server = Server.withPort(0).serve("/test.properties").withBehaviours(error(404)).start();

    AbstractRemoteDiscoveryStrategy underTest = create();

    AbstractRemoteDiscoveryStrategy.RequestResult result =
        underTest.handleRequest(server.getUrl().toString() + "/test.properties");

    assertThat(result, nullValue());
  }

  @Test
  public void testProxyFails()
      throws Exception
  {
    server = Proxy.withPort(0).serve("/test.properties").withBehaviours(error(404)).start();

    final DefaultRemoteHttpProxySettings httpProxySettings = new DefaultRemoteHttpProxySettings();
    httpProxySettings.setHostname("localhost");
    httpProxySettings.setPort(server.getPort());

    when(remoteProxySettings.getHttpProxySettings()).thenReturn(httpProxySettings);

    AbstractRemoteDiscoveryStrategy underTest = create();

    result = underTest.handleRequest("http://invalid.url/test.properties");

    assertThat(result, nullValue());
  }

  @Test
  public void testDirectRedirect()
      throws Exception
  {
    server =
        Server.withPort(0).serve("/test.properties").withBehaviours(
            redirect("/redirect/test.properties", 301)).serve("/redirect/test.properties").withBehaviours(
            content(content)).start();

    AbstractRemoteDiscoveryStrategy underTest = create();

    AbstractRemoteDiscoveryStrategy.RequestResult result =
        underTest.handleRequest(server.getUrl().toString() + "/test.properties");

    assertThat(result.getInputStream(), notNullValue());
    assertThat("content did not match",
        IOUtils.contentEquals(result.getInputStream(), new ByteArrayInputStream(content.getBytes())), is(true));
  }

  @Test
  public void testProxyRedirect()
      throws Exception
  {
    server =
        Proxy.withPort(0).serve("/test.properties").withBehaviours(redirect("/redirect/test.properties", 301)).serve(
            "/redirect/test.properties").withBehaviours(content(content)).start();

    final DefaultRemoteHttpProxySettings httpProxySettings = new DefaultRemoteHttpProxySettings();
    httpProxySettings.setHostname("localhost");
    httpProxySettings.setPort(server.getPort());

    when(remoteProxySettings.getHttpProxySettings()).thenReturn(httpProxySettings);

    AbstractRemoteDiscoveryStrategy underTest = create();

    result = underTest.handleRequest("http://invalid.url/test.properties");

    assertThat(result.getInputStream(), notNullValue());
    assertThat("content did not match",
        IOUtils.contentEquals(result.getInputStream(), new ByteArrayInputStream(content.getBytes())), is(true));
  }

}
