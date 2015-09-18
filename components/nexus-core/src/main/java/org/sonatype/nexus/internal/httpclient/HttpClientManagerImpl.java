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
package org.sonatype.nexus.internal.httpclient;

import java.io.IOException;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.Mutex;
import org.sonatype.nexus.common.app.NexusInitializedEvent;
import org.sonatype.nexus.common.app.NexusStoppedEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.httpclient.GlobalHttpClientConfigurationChanged;
import org.sonatype.nexus.httpclient.HttpClientConfigurationStore;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.HttpClientPlan;
import org.sonatype.nexus.httpclient.HttpClientPlan.Customizer;
import org.sonatype.nexus.httpclient.config.ConfigurationCustomizer;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Default {@link HttpClientManager}.
 *
 * @since 3.0
 */
@SuppressWarnings("PackageAccessibility") // FIXME: httpclient usage is producing lots of OSGI warnings in IDEA
@Named
@Singleton
public class HttpClientManagerImpl
    extends StateGuardLifecycleSupport
    implements HttpClientManager, EventAware
{
  static final String HTTPCLIENT_OUTBOUND_LOGGER_NAME = "org.sonatype.nexus.httpclient.outbound";

  private static final String CTX_REQ_STOPWATCH = "request.stopwatch";

  private final Logger outboundLog = LoggerFactory.getLogger(HTTPCLIENT_OUTBOUND_LOGGER_NAME);

  private final EventBus eventBus;

  private final HttpClientConfigurationStore store;

  private final Provider<HttpClientConfiguration> defaults;

  private final SharedHttpClientConnectionManager sharedConnectionManager;

  private final DefaultsCustomizer defaultsCustomizer;

  private final Mutex lock = new Mutex();

  private HttpClientConfiguration configuration;

  @Inject
  public HttpClientManagerImpl(final EventBus eventBus,
                               final HttpClientConfigurationStore store,
                               @Named("initial") final Provider<HttpClientConfiguration> defaults,
                               final SharedHttpClientConnectionManager sharedConnectionManager,
                               final DefaultsCustomizer defaultsCustomizer)
  {
    this.eventBus = checkNotNull(eventBus);

    this.store = checkNotNull(store);
    log.debug("Store: {}", store);

    this.defaults = checkNotNull(defaults);
    log.debug("Defaults: {}", defaults);

    this.sharedConnectionManager = checkNotNull(sharedConnectionManager);
    this.defaultsCustomizer = checkNotNull(defaultsCustomizer);
  }

  //
  // Lifecycle
  //

  @Override
  protected void doStart() throws Exception {
    sharedConnectionManager.start();
  }

  @Override
  protected void doStop() throws Exception {
    sharedConnectionManager.stop();
  }

  @Subscribe
  public void on(final NexusInitializedEvent event) throws Exception {
    start();
  }

  @Subscribe
  public void on(final NexusStoppedEvent event) throws Exception {
    stop();
  }

  //
  // Configuration
  //

  /**
   * Load configuration from store, or use defaults.
   */
  private HttpClientConfiguration loadConfiguration() {
    HttpClientConfiguration model = store.load();

    // use defaults if no configuration was loaded from the store
    if (model == null) {
      model = defaults.get();

      // default config must not be null
      checkNotNull(model);

      log.info("Using default configuration: {}", model);
    }
    else {
      log.info("Loaded configuration: {}", model);
    }

    return model;
  }

  /**
   * Return configuration, loading if needed.
   *
   * The result model should be considered _immutable_ unless copied.
   */
  private HttpClientConfiguration getConfigurationInternal() {
    synchronized (lock) {
      if (configuration == null) {
        configuration = loadConfiguration();
      }
      return configuration;
    }
  }

  /**
   * Return _copy_ of configuration.
   */
  @Override
  @Guarded(by = STARTED)
  public HttpClientConfiguration getConfiguration() {
    return getConfigurationInternal().copy();
  }

  @Override
  @Guarded(by = STARTED)
  public void setConfiguration(final HttpClientConfiguration configuration) {
    checkNotNull(configuration);

    HttpClientConfiguration model = configuration.copy();
    log.info("Saving configuration: {}", model);
    synchronized (lock) {
      store.save(model);
      this.configuration = model;
    }

    // fire event to inform that the global configuration changed
    eventBus.post(new GlobalHttpClientConfigurationChanged());
  }

  //
  // Instance creation
  //

  @Override
  @Guarded(by = STARTED)
  public CloseableHttpClient create(final @Nullable HttpClientPlan.Customizer customizer) {
    return prepare(customizer).build();
  }

  @Override
  @Guarded(by = STARTED)
  public CloseableHttpClient create() {
    // create with defaults only
    return create(null);
  }

  @Override
  @Guarded(by = STARTED)
  public HttpClientBuilder prepare(final @Nullable Customizer customizer) {
    final HttpClientPlan plan = new HttpClientPlan();

    // attach connection manager early, so customizer has chance to replace it if needed
    plan.getClient().setConnectionManager(sharedConnectionManager);

    // apply defaults
    defaultsCustomizer.customize(plan);

    // apply globals
    new ConfigurationCustomizer(getConfigurationInternal()).customize(plan);

    // apply instance customization
    if (customizer != null) {
      customizer.customize(plan);
    }

    // apply plan to builder
    HttpClientBuilder builder = plan.getClient();
    builder.setDefaultConnectionConfig(plan.getConnection().build());
    builder.setDefaultSocketConfig(plan.getSocket().build());
    builder.setDefaultRequestConfig(plan.getRequest().build());
    builder.setDefaultCredentialsProvider(plan.getCredentials());

    builder.addInterceptorFirst(new HttpRequestInterceptor()
    {
      @Override
      public void process(final HttpRequest request, final HttpContext context)
          throws HttpException, IOException
      {
        // add custom http-context attributes
        for (Entry<String, Object> entry : plan.getAttributes().entrySet()) {
          // only set context attribute if not already set, to allow per request overrides
          if (context.getAttribute(entry.getKey()) == null) {
            context.setAttribute(entry.getKey(), entry.getValue());
          }
        }

        // add custom http-request headers
        for (Entry<String, String> entry : plan.getHeaders().entrySet()) {
          request.addHeader(entry.getKey(), entry.getValue());
        }
      }
    });
    builder.addInterceptorLast(new HttpRequestInterceptor()
    {
      @Override
      public void process(final HttpRequest httpRequest, final HttpContext httpContext)
          throws HttpException, IOException
      {
        if (outboundLog.isDebugEnabled()) {
          httpContext.setAttribute(CTX_REQ_STOPWATCH, Stopwatch.createStarted());
          HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
          outboundLog.debug("{} > {}", clientContext.getTargetHost(), httpRequest.getRequestLine());
        }
      }
    });
    builder.addInterceptorLast(new HttpResponseInterceptor()
    {
      @Override
      public void process(final HttpResponse httpResponse, final HttpContext httpContext)
          throws HttpException, IOException
      {
        Stopwatch stopwatch = (Stopwatch) httpContext.getAttribute(CTX_REQ_STOPWATCH);
        if (stopwatch != null) {
          HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
          outboundLog.debug("{} < {} @ {}", clientContext.getTargetHost(), httpResponse.getStatusLine(), stopwatch);
        }
      }
    });

    return builder;
  }
}
