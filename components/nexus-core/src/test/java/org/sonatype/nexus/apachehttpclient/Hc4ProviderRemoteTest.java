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
package org.sonatype.nexus.apachehttpclient;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.google.common.collect.Sets;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.fest.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * UTs that include actual remote access against mock HTTP Server (Jetty in this case).
 */
public class Hc4ProviderRemoteTest
    extends TestSupport
{
  private static final String UA = "TheUserAgent";

  private Hc4ProviderImpl testSubject;

  private UserAgentChecker userAgentChecker;

  private Server server;

  @Mock
  private ApplicationConfiguration applicationConfiguration;

  @Mock
  private UserAgentBuilder userAgentBuilder;

  @Mock
  private EventBus eventBus;

  @Mock
  private RemoteStorageContext globalRemoteStorageContext;

  @Mock
  private RemoteProxySettings remoteProxySettings;

  @Mock
  private RemoteHttpProxySettings remoteHttpProxySettings;

  @Mock
  private PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

  @Before
  public void prepare() throws Exception {
    userAgentChecker = new UserAgentChecker();
    final int port;
    try (final ServerSocket ss = new ServerSocket(0)) {
      port = ss.getLocalPort();
    }
    server = new Server(port);
    server.setHandler(userAgentChecker);
    server.start();

    final DefaultRemoteConnectionSettings rcs = new DefaultRemoteConnectionSettings();
    when(globalRemoteStorageContext.getRemoteConnectionSettings()).thenReturn(rcs);
    when(globalRemoteStorageContext.getRemoteProxySettings()).thenReturn(remoteProxySettings);
    when(remoteProxySettings.getHttpProxySettings()).thenReturn(remoteHttpProxySettings);
    when(remoteProxySettings.getHttpsProxySettings()).thenReturn(remoteHttpProxySettings);

    // jetty acts as proxy
    when(remoteHttpProxySettings.isEnabled()).thenReturn(true);
    when(remoteHttpProxySettings.getHostname()).thenReturn("localhost");
    when(remoteHttpProxySettings.getPort()).thenReturn(port);

    when(userAgentBuilder.formatUserAgentString(Matchers.<RemoteStorageContext>any())).thenReturn(UA);
    when(applicationConfiguration.getGlobalRemoteStorageContext()).thenReturn(globalRemoteStorageContext);
  }

  @After
  public void cleanup() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void httpsTunnelConnectUserAgentIsSent() throws Exception {
    try {
      testSubject = new Hc4ProviderImpl(applicationConfiguration, userAgentBuilder, eventBus, jmxInstaller, null);
      final HttpClient client = testSubject.createHttpClient();
      final HttpGet get = new HttpGet("https://www.somehost.com/");
      final HttpResponse response = client.execute(get);
      assertThat(response.getStatusLine().getStatusCode(), equalTo(404)); // does not matter actually
      assertThat(userAgentChecker.getUserAgents(), hasSize(1)); // same UA should be used even if multiple reqs
      assertThat(userAgentChecker.getUserAgents().iterator().next(), equalTo(UA)); // the one we set must be used
    }
    finally {
      testSubject.shutdown();
    }
  }

  // ==

  public static class UserAgentChecker
      extends AbstractHandler
  {
    public static final String NO_AGENT = "";

    private final Set<String> userAgents = Sets.newHashSet();

    public Set<String> getUserAgents() {
      return userAgents;
    }

    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request,
                       final HttpServletResponse response)
        throws IOException, ServletException
    {
      final String ua = request.getHeader("user-agent");
      if (Strings.isNullOrEmpty(ua)) {
        userAgents.add(NO_AGENT);
      }
      else {
        userAgents.add(ua);
      }
    }
  }
}
