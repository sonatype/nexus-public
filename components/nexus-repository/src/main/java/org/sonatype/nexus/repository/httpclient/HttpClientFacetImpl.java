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
package org.sonatype.nexus.repository.httpclient;

import java.io.IOException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;

import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.httpclient.GlobalHttpClientConfigurationChanged;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.AuthenticationConfigurationDeserializer;
import org.sonatype.nexus.httpclient.config.ConfigurationCustomizer;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import org.apache.http.client.HttpClient;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;

/**
 * Default {@link HttpClientFacet} implementation.
 *
 * @since 3.0
 */
@Named
@SuppressWarnings("PackageAccessibility") // FIXME: httpclient usage is producing lots of OSGI warnings in IDEA
public class HttpClientFacetImpl
    extends FacetSupport
    implements HttpClientFacet
{
  private final HttpClientManager httpClientManager;

  @VisibleForTesting
  static final String CONFIG_KEY = "httpclient";

  @VisibleForTesting
  static class Config
  {
    @Valid
    @Nullable
    public ConnectionConfiguration connection;

    @Valid
    @Nullable
    @JsonDeserialize(using = AuthenticationConfigurationDeserializer.class)
    public AuthenticationConfiguration authentication;

    @Nullable
    public Boolean blocked;

    @Nullable
    public Boolean autoBlock;
  }

  private Config config;

  private FilteredHttpClient httpClient;

  @Inject
  public HttpClientFacetImpl(final HttpClientManager httpClientManager) {
    this.httpClientManager = checkNotNull(httpClientManager);
  }

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);
    log.debug("Config: {}", config);

    createHttpClient();
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  protected void doStop() throws Exception {
    closeHttpClient();
  }

  @Override
  @Guarded(by = STARTED)
  public HttpClient getHttpClient() {
    return checkNotNull(httpClient);
  }

  @Override
  @Guarded(by = STARTED)
  public RemoteConnectionStatus getStatus() {
    return httpClient.getStatus();
  }

  @Subscribe
  public void on(final GlobalHttpClientConfigurationChanged event) throws IOException {
    closeHttpClient();
    createHttpClient();
  }

  private void createHttpClient() {
    // construct http client delegate
    HttpClientConfiguration delegateConfig = new HttpClientConfiguration();
    delegateConfig.setConnection(config.connection);
    delegateConfig.setAuthentication(config.authentication);
    HttpClient delegate = httpClientManager.create(new ConfigurationCustomizer(delegateConfig));

    // wrap delegate with auto-block aware client
    httpClient = new FilteredHttpClient(delegate, config);
    log.debug("Created HTTP client: {}", httpClient);
  }

  private void closeHttpClient() throws IOException {
    log.debug("Closing HTTP client: {}", httpClient);
    httpClient.close();
    httpClient = null;
  }
}
