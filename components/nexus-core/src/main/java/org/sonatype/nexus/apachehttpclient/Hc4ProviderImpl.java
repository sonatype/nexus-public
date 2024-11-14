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

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.ConfigurationLoadEvent;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.proxy.events.NexusStoppedEvent;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.utils.UserAgentBuilder;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Default implementation of {@link Hc4Provider}.
 *
 * @author cstamas
 * @since 2.2
 */
@Singleton
@Named
public class Hc4ProviderImpl
    extends Hc4ProviderBase
    implements Hc4Provider
{

  /**
   * Key for customizing connection pool maximum size. Value should be integer equal to 0 or greater. Pool size of 0
   * will actually prevent use of pool. Any positive number means the actual size of the pool to be created. This is
   * a
   * hard limit, connection pool will never contain more than this count of open sockets.
   */
  private static final String CONNECTION_POOL_MAX_SIZE_KEY = "nexus.apacheHttpClient4x.connectionPoolMaxSize";

  /**
   * Default pool max size: 200.
   */
  private static final int CONNECTION_POOL_MAX_SIZE_DEFAULT = 200;

  /**
   * Key for customizing connection pool size per route (usually per-repository, but not quite in case of Mirrors).
   * Value should be integer equal to 0 or greater. Pool size of 0 will actually prevent use of pool. Any positive
   * number means the actual size of the pool to be created.
   */
  private static final String CONNECTION_POOL_SIZE_KEY = "nexus.apacheHttpClient4x.connectionPoolSize";

  /**
   * Default pool size: 20.
   */
  private static final int CONNECTION_POOL_SIZE_DEFAULT = 20;

  /**
   * Key for customizing connection pool idle time. In other words, how long open connections (sockets) are kept in
   * pool idle (unused) before they get evicted and closed. Value is milliseconds.
   */
  private static final String CONNECTION_POOL_IDLE_TIME_KEY = "nexus.apacheHttpClient4x.connectionPoolIdleTime";

  /**
   * Default pool idle time: 30 seconds.
   */
  private static final long CONNECTION_POOL_IDLE_TIME_DEFAULT = TimeUnit.SECONDS.toMillis(30);

  /**
   * Key for customizing period of inactivity after which persistent connections must be re-validated
   * prior to being leased to the consumer.
   * Value is milliseconds.
   */
  private static final String CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY_TIME_KEY =
      "nexus.apacheHttpClient4x.validateAfterInactivityTime";

  /**
   * Default period of inactivity: 2 seconds.
   */
  private static final long CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY_TIME_DEFAULT = TimeUnit.SECONDS.toMillis(2);

  /**
   * Key for customizing connection pool timeout. In other words, how long should a HTTP request execution be blocked
   * when pool is depleted, for a connection. Value is milliseconds.
   */
  private static final String CONNECTION_POOL_TIMEOUT_KEY = "nexus.apacheHttpClient4x.connectionPoolTimeout";

  /**
   * Default pool timeout: 30 seconds.
   */
  private static final long CONNECTION_POOL_TIMEOUT_DEFAULT = TimeUnit.SECONDS.toMillis(30);

  // ==

  /**
   * Application configuration holding the {@link GlobalRemoteConnectionSettings}.
   */
  private final ApplicationConfiguration applicationConfiguration;

  /**
   * The low level core event bus.
   */
  private final EventBus eventBus;

  /**
   * Shared client connection manager.
   */
  private final ManagedClientConnectionManager sharedConnectionManager;

  /**
   * Thread evicting idle open connections from {@link #sharedConnectionManager}.
   */
  private final EvictingThread evictingThread;

  /**
   * Used to install created {@link PoolingClientConnectionManager} into jmx.
   */
  private final PoolingClientConnectionManagerMBeanInstaller jmxInstaller;

  /**
   * @param applicationConfiguration the Nexus {@link ApplicationConfiguration}, must not be {@code null}.
   * @param userAgentBuilder         UA builder component, must not be {@code null}.
   * @param eventBus                 the event bus, must not be {@code null}.
   * @param jmxInstaller             installer to expose pool information over JMX, must not be {@code null}.
   * @param selectors                list of {@link SSLContextSelector}, might be {@code null}.
   */
  @Inject
  public Hc4ProviderImpl(final ApplicationConfiguration applicationConfiguration,
                         final UserAgentBuilder userAgentBuilder, final EventBus eventBus,
                         final PoolingClientConnectionManagerMBeanInstaller jmxInstaller,
                         final List<SSLContextSelector> selectors)
  {
    super(userAgentBuilder);
    this.applicationConfiguration = Preconditions.checkNotNull(applicationConfiguration);
    this.jmxInstaller = Preconditions.checkNotNull(jmxInstaller);
    this.sharedConnectionManager = createClientConnectionManager(selectors);
    this.evictingThread = new EvictingThread(sharedConnectionManager, getConnectionPoolIdleTime());
    this.evictingThread.start();
    this.eventBus = Preconditions.checkNotNull(eventBus);
    this.eventBus.register(this);
    this.jmxInstaller.register(sharedConnectionManager);
    log.info(
        "Started (connectionPoolMaxSize {}, connectionPoolSize {}, connectionPoolIdleTime {} ms, " +
            "connectionPoolTimeout {} ms, keepAliveMaxDuration {} ms, validateAfterInactivityTime {} ms)",
        getConnectionPoolMaxSize(),
        getConnectionPoolSize(),
        getConnectionPoolIdleTime(),
        getConnectionPoolTimeout(),
        getKeepAliveMaxDuration(),
        getValidateAfterInactivityTime()
    );
  }

  // configuration

  /**
   * Returns the pool max size.
   */
  protected int getConnectionPoolMaxSize() {
    return SystemPropertiesHelper.getInteger(CONNECTION_POOL_MAX_SIZE_KEY, CONNECTION_POOL_MAX_SIZE_DEFAULT);
  }

  /**
   * Returns the pool size per route.
   */
  protected int getConnectionPoolSize() {
    return SystemPropertiesHelper.getInteger(CONNECTION_POOL_SIZE_KEY, CONNECTION_POOL_SIZE_DEFAULT);
  }

  /**
   * Returns the connection pool idle (idle as unused but pooled) time in milliseconds.
   */
  protected long getConnectionPoolIdleTime() {
    return SystemPropertiesHelper.getLong(CONNECTION_POOL_IDLE_TIME_KEY, CONNECTION_POOL_IDLE_TIME_DEFAULT);
  }

  /**
   * Get period of inactivity in milliseconds after which persistent connections must be re-validated
   * prior to being leased to the consumer.
   */
  protected long getValidateAfterInactivityTime() {
    return SystemPropertiesHelper.getLong(
        CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY_TIME_KEY,
        CONNECTION_POOL_VALIDATE_AFTER_INACTIVITY_TIME_DEFAULT);
  }

  /**
   * Returns the pool timeout in milliseconds.
   */
  protected long getConnectionPoolTimeout() {
    return SystemPropertiesHelper.getLong(CONNECTION_POOL_TIMEOUT_KEY, CONNECTION_POOL_TIMEOUT_DEFAULT);
  }

  // ==

  /**
   * Performs a clean shutdown on this component, it kills the evicting thread and shuts down the shared connection
   * manager. Multiple invocation of this method is safe, it will not do anything.
   */
  public synchronized void shutdown() {
    evictingThread.interrupt();
    jmxInstaller.unregister();
    sharedConnectionManager._shutdown();
    eventBus.unregister(this);
    log.info("Stopped");
  }

  @Subscribe
  public void onEvent(final NexusStoppedEvent evt) {
    shutdown();
  }

  // ==

  /**
   * Safety net to prevent thread leaks (in non-production environment, mainly for ITs or UTs).
   */
  @Override
  protected void finalize()
      throws Throwable
  {
    try {
      shutdown();
    }
    finally {
      super.finalize();
    }
  }

  // == Hc4Provider API

  @Override
  public HttpClient createHttpClient() {
    // connection manager will cap the max count of connections, but with this below
    // we get rid of pooling. Pooling is used in Proxy repositories only, as all other
    // components using the "shared" httpClient should not produce hiw rate of requests
    // anyway, as they usually happen per user interactions (GPG gets keys are staging repo is closed, if not cached
    // yet, LVO gets info when UI's main window is loaded into user's browser, etc
    // ==
    // NEXUS-6220: This story above is mainly true and is basically "resource optimization", as
    // this method is used by various "side services" (typically LVO etc), where single request is made
    // with huge pauses in between. Still, connection reuse is needed in some rare cases,
    // like when you have NTLM proxy in between Nexus and the Internet. So, let ask HC4 provider,
    // does it "think" we still need connection reuse or not.
    return createHttpClient(reuseConnectionsNeeded(applicationConfiguration.getGlobalRemoteStorageContext()));
  }

  @Override
  public HttpClient createHttpClient(final boolean reuseConnections) {
    final Builder builder = prepareHttpClient(applicationConfiguration.getGlobalRemoteStorageContext());
    if (!reuseConnections) {
      builder.getHttpClientBuilder().setConnectionReuseStrategy(new NoConnectionReuseStrategy());
    }
    return builder.build();
  }

  @Override
  public HttpClient createHttpClient(final RemoteStorageContext context) {
    return prepareHttpClient(context).build();
  }

  @Override
  public Builder prepareHttpClient(final RemoteStorageContext context) {
    return prepareHttpClient(context, sharedConnectionManager);
  }

  // ==

  @Override
  protected void applyConfig(final Builder builder, final RemoteStorageContext context) {
    super.applyConfig(builder, context);
    builder.getRequestConfigBuilder().setConnectionRequestTimeout(Ints.checkedCast(getConnectionPoolTimeout()));
  }

  protected ManagedClientConnectionManager createClientConnectionManager(
      final List<SSLContextSelector> selectors)
      throws IllegalStateException
  {
    final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", new NexusSSLConnectionSocketFactory(
            (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
            SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER, selectors)).build();

    final ManagedClientConnectionManager connManager = new ManagedClientConnectionManager(registry);
    final int maxConnectionCount = getConnectionPoolMaxSize();
    final int perRouteConnectionCount = Math.min(getConnectionPoolSize(), maxConnectionCount);

    connManager.setMaxTotal(maxConnectionCount);
    connManager.setDefaultMaxPerRoute(perRouteConnectionCount);

    return connManager;
  }

  @Subscribe
  public void on(final ConfigurationLoadEvent event) {
    RemoteStorageContext storageContext = event.getApplicationConfiguration().getGlobalRemoteStorageContext();
    if (storageContext != null) {
      sharedConnectionManager.setConnectionTimeout(getConnectionTimeout(storageContext));
    }
  }

  private class ManagedClientConnectionManager
      extends PoolingHttpClientConnectionManager
  {
    public ManagedClientConnectionManager(final Registry<ConnectionSocketFactory> schemeRegistry) {
      super(schemeRegistry);
      setValidateAfterInactivity((int) getValidateAfterInactivityTime());
    }

    public void setConnectionTimeout(final int connectionTimeout) {
      setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(connectionTimeout).build());
    }

    @Override
    public void shutdown() {
      // do nothing in order to avoid unwanted shutdown of shared connection manager
    }

    private void _shutdown() {
      super.shutdown();
    }
  }

}
