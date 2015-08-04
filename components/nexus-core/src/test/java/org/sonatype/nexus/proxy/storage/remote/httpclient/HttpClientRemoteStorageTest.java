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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.apachehttpclient.Hc4ProviderImpl;
import org.sonatype.nexus.apachehttpclient.PoolingClientConnectionManagerMBeanInstaller;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.RemoteStorageException;
import org.sonatype.nexus.proxy.RemoteStorageTransportOverloadedException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteProxySettings;
import org.sonatype.nexus.proxy.storage.remote.DefaultRemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.RemoteItemNotFoundException;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.http.QueryStringBuilder;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.sisu.goodies.common.Time;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;
import org.apache.http.message.BasicHeader;
import com.google.inject.util.Providers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link HttpClientRemoteStorage} UTs.
 *
 * @since 2.2
 */
public class HttpClientRemoteStorageTest
    extends TestSupport
{

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * When retrieving an item with a path that ends with a "/" and without a query, an RemoteItemNotFoundException
   * with
   * a message that a collection could not be downloaded over HTTP should be thrown.
   */
  @Test
  public void retrieveCollectionWhenPathEndsWithSlashAndNoQuery()
      throws Exception
  {
    final HttpClientRemoteStorage underTest =
        new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
            mock(MimeSupport.class), mock(QueryStringBuilder.class), mock(HttpClientManager.class));
    final ProxyRepository proxyMock = mock(ProxyRepository.class);
    when(proxyMock.getId()).thenReturn("id");
    when(proxyMock.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));

    thrown.expect(RemoteItemNotFoundException.class);
    thrown.expectMessage("not found in remote storage of repository");

    underTest.retrieveItem(proxyMock, new ResourceStoreRequest("bar/"), "http://foo.com");
  }

  /**
   * When retrieving an item with a path that ends with a "/" and a query string that ends with a "/", an
   * RemoteItemNotFoundException with a message that a collection could not be downloaded over HTTP should be thrown.
   */
  @Test
  public void retrieveCollectionWhenPathEndsWithSlashAndQueryEndsWithSlash()
      throws Exception
  {
    final HttpClientRemoteStorage underTest =
        new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
            mock(MimeSupport.class), mock(QueryStringBuilder.class), mock(HttpClientManager.class));
    final ProxyRepository proxyMock = mock(ProxyRepository.class);
    when(proxyMock.getId()).thenReturn("id");
    when(proxyMock.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));

    thrown.expect(RemoteItemNotFoundException.class);
    thrown.expectMessage("not found in remote storage of repository");

    underTest.retrieveItem(proxyMock, new ResourceStoreRequest("bar/?param=x/"),
        "http://foo.com");
  }

  /**
   * When retrieving an item with a path that ends with a "/" and a query string that does not end with a "/", an
   * RemoteItemNotFoundException with a message that a collection could not be downloaded over HTTP should be thrown.
   */
  @Test
  public void retrieveCollectionWhenPathEndsWithSlashAndQueryDoesNotEndWithSlash()
      throws Exception
  {
    final HttpClientRemoteStorage underTest =
        new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
            mock(MimeSupport.class), mock(QueryStringBuilder.class), mock(HttpClientManager.class));
    final ProxyRepository proxyMock = mock(ProxyRepository.class);
    when(proxyMock.getId()).thenReturn("id");
    when(proxyMock.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));

    thrown.expect(RemoteItemNotFoundException.class);
    thrown.expectMessage("not found in remote storage of repository");

    underTest.retrieveItem(proxyMock, new ResourceStoreRequest("bar/?param=x"),
        "http://foo.com");
  }

  /**
   * When retrieving an item with a path that does not end with a "/" and a query string that does not end with a
   * "/",
   * no exception should be thrown.
   */
  @Test
  public void retrieveCollectionWhenPathDoesNotEndWithSlashAndQueryDoesNotEndWithSlash()
      throws Exception
  {
    final HttpClientRemoteStorage underTest =
        new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
            mock(MimeSupport.class), mock(QueryStringBuilder.class), mock(HttpClientManager.class))
        {
          @Override
          HttpResponse executeRequest(final ProxyRepository repository, final ResourceStoreRequest request,
                                      final HttpUriRequest httpRequest, final String baseUrl, final boolean contentRelated)
              throws RemoteStorageException
          {
            final HttpResponse httpResponse = mock(HttpResponse.class);
            final StatusLine statusLine = mock(StatusLine.class);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(200);
            when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
            return httpResponse;
          }
        };

    final ProxyRepository proxyMock = mock(ProxyRepository.class);
    when(proxyMock.getId()).thenReturn("foo");
    when(proxyMock.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));

    underTest.retrieveItem(proxyMock, new ResourceStoreRequest("bar?param=x"), "http://foo.com");
  }

  /**
   * When pool is depleted, and underlying HttpClient4x cannot fulfil request due to
   * {@link ConnectionPoolTimeoutException}, the {@link HttpClientRemoteStorage} should throw a new exception,
   * instance of {@link RemoteStorageTransportOverloadedException} to mark to core that transport is overloaded.
   */
  @Test
  public void emitProperExceptionOnPoolDepletion()
      throws Exception
  {
    setParameters();
    Hc4ProviderImpl hc4Provider = null;
    try {
      // the foreplay: setting up
      final RemoteStorageContext globalRemoteStorageContext = new DefaultRemoteStorageContext(null);
      final DefaultRemoteConnectionSettings rcs = new DefaultRemoteConnectionSettings();
      rcs.setConnectionTimeout(86400000);
      globalRemoteStorageContext.setRemoteConnectionSettings(new DefaultRemoteConnectionSettings());
      globalRemoteStorageContext.setRemoteProxySettings(mock(RemoteProxySettings.class));
      final ApplicationConfiguration applicationConfiguration = mock(ApplicationConfiguration.class);
      when(applicationConfiguration.getGlobalRemoteStorageContext()).thenReturn(globalRemoteStorageContext);

      // real provider and initializing it with NexusStarted event
      hc4Provider =
          new Hc4ProviderImpl(applicationConfiguration, mock(UserAgentBuilder.class),
              mock(EventBus.class),
              mock(PoolingClientConnectionManagerMBeanInstaller.class),
              null);

      // the RRS instance we test
      final HttpClientRemoteStorage underTest =
          new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
              mock(MimeSupport.class), mock(QueryStringBuilder.class), new HttpClientManagerImpl(
              hc4Provider, mock(UserAgentBuilder.class)));

      // a mock proxy repository with some mocks to make RRS work
      final RemoteStorageContext proxyContext = new DefaultRemoteStorageContext(globalRemoteStorageContext);
      final ProxyRepository repository = mock(ProxyRepository.class);
      when(repository.getId()).thenReturn("foo");
      when(repository.getName()).thenReturn("foo");
      when(repository.getRemoteUrl()).thenReturn("http://www.somehost.com/");
      when(repository.getRemoteStorageContext()).thenReturn(proxyContext);

      // a mock remote server that will simply "hang" to occupy the request socket
      final Server server =
          Server.withPort(0).serve("/").withBehaviours(Behaviours.pause(Time.days(1))).start();
      // the URL we will try to connect to
      final String url = "http://foo.com:" + server.getPort() + "/foo/bar.jar";
      // the requesting logic packed as Runnable
      final Runnable request = new RequesterRunnable(underTest, repository, url);
      try {
        // we fire 1st request as a Thread, this thread will be blocked as Server will simply "pause"
        // this also means, that connection stays leased from pool, and since pool size is 1, we
        // intentionally depleted the connection pool (reached max connection count)
        final Thread blockedThread = new Thread(request);
        blockedThread.start();

        // give some time to thread above
        Thread.sleep(200);

        try {
          // in current thread we try to establish 2nd connection
          // this here will need to fail, as connection pool is depleted
          // ConnectionPoolTimeoutException should be thrown by HC4
          // that RRS "repackages" into RemoteStorageTransportOverloadedException
          request.run();

          // fail if no exception
          Assert.fail("RemoteStorageTransportOverloadedException expected!");
        }
        catch (IllegalStateException e) {
          Assert.assertNotNull("We except the cause be RemoteStorageTransportOverloadedException!",
              e.getCause());
          Assert.assertEquals(RemoteStorageTransportOverloadedException.class, e.getCause().getClass());
        }
      }
      finally {
        server.stop();
      }
    }
    finally {
      if (hc4Provider != null) {
        hc4Provider.shutdown();
      }
      unsetParameters();
    }
  }

  /**
   * When checking for repository remote availability (newerThen is 0), we should neglect response carried 
   * last-modified time as we are not interested in it.
   * 
   * @see https://issues.sonatype.org/browse/NEXUS-6701
   */
  @Test
  public void checkRepositoryRemoteAvailabilityNeglectLastModified()
      throws Exception
  {
    final HttpClientRemoteStorage underTest =
        new HttpClientRemoteStorage(mock(ApplicationStatusSource.class),
            mock(MimeSupport.class), mock(QueryStringBuilder.class), mock(HttpClientManager.class))
        {
          @Override
          HttpResponse executeRequest(final ProxyRepository repository, final ResourceStoreRequest request,
                                      final HttpUriRequest httpRequest, final String baseUrl, final boolean contentRelated)
              throws RemoteStorageException
          {
            final HttpResponse httpResponse = mock(HttpResponse.class);
            when(httpResponse.getFirstHeader("last-modified")).thenReturn(new BasicHeader("last-modified", "Thu, 01 Jan 1970 00:00:00 GMT"));
            final StatusLine statusLine = mock(StatusLine.class);
            when(httpResponse.getStatusLine()).thenReturn(statusLine);
            when(statusLine.getStatusCode()).thenReturn(200);
            when(httpResponse.getEntity()).thenReturn(mock(HttpEntity.class));
            return httpResponse;
          }
        };

    final ProxyRepository proxyMock = mock(ProxyRepository.class);
    when(proxyMock.getId()).thenReturn("foo");
    when(proxyMock.getRemoteUrl()).thenReturn("http://repo1.maven.org/maven2/");
    when(proxyMock.getRemoteStorageContext()).thenReturn(new DefaultRemoteStorageContext(null));
    
    assertThat(underTest.checkRemoteAvailability(0, proxyMock, new ResourceStoreRequest("/"), false), is(true));
    assertThat(underTest.checkRemoteAvailability(System.currentTimeMillis(), proxyMock, new ResourceStoreRequest("/"), false), is(false));
  }

  protected void setParameters() {
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize", "1");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolSize", "1");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolKeepalive", "86400000");
    System.setProperty("nexus.apacheHttpClient4x.connectionPoolTimeout", "100");
  }

  protected void unsetParameters() {
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolMaxSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolSize");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolKeepalive");
    System.clearProperty("nexus.apacheHttpClient4x.connectionPoolTimeout");
  }

  private static class RequesterRunnable
      implements Runnable
  {
    private final HttpClientRemoteStorage underTest;

    private final ProxyRepository repository;

    private final String url;

    private RequesterRunnable(HttpClientRemoteStorage underTest, ProxyRepository repository, String url) {
      this.underTest = underTest;
      this.repository = repository;
      this.url = url;
    }

    @Override
    public void run() {
      try {
        underTest.retrieveItem(repository, new ResourceStoreRequest("foo/bar.jar"), url);
      }
      catch (Exception e) {
        throw new IllegalStateException("Failed!", e);
      }
    }
  }
}
