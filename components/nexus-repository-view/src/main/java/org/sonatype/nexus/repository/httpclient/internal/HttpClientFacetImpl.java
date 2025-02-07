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
package org.sonatype.nexus.repository.httpclient.internal;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;

import com.google.auth.oauth2.GoogleCredentials;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.distributed.event.service.api.common.RepositoryRemoteConnectionStatusEvent;
import org.sonatype.nexus.httpclient.HttpClientManager;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.BearerTokenAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.ConfigurationCustomizer;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.httpclient.config.GoogleAuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfigurationChangedEvent;
import org.sonatype.nexus.httpclient.config.UsernameAuthenticationConfiguration;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.httpclient.AutoBlockConfiguration;
import org.sonatype.nexus.repository.httpclient.ContentCompressionStrategy;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.httpclient.NormalizationStrategy;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatus;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusEvent;
import org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusObserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.message.BasicHeader;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.AUTO_BLOCKED_UNAVAILABLE;
import static org.sonatype.nexus.repository.httpclient.RemoteConnectionStatusType.UNINITIALISED;

/**
 * Default {@link HttpClientFacet} implementation.
 *
 * @since 3.0
 */
@Named
public class HttpClientFacetImpl
    extends FacetSupport
    implements HttpClientFacet, RemoteConnectionStatusObserver
{
  private static final String DEFAULT = "default";

  private final HttpClientManager httpClientManager;

  @VisibleForTesting
  static final String CONFIG_KEY = "httpclient";

  private final Map<String, AutoBlockConfiguration> autoBlockConfiguration;

  private final Map<String, RedirectStrategy> redirectStrategy;

  private final Map<String, NormalizationStrategy> normalizationStrategies;

  private final Map<String, ContentCompressionStrategy> contentCompressionStrategies;

  private final Map<String, TargetAuthenticationStrategy> authenticationStrategies;

  private String unencryptedPassword;

  @VisibleForTesting
  public static class Config
  {
    @Valid
    @Nullable
    public ConnectionConfiguration connection;

    @Valid
    @Nullable
    public AuthenticationConfiguration authentication;

    @Nullable
    public Boolean blocked;

    @Nullable
    public Boolean autoBlock;
  }

  private Config config;

  @VisibleForTesting
  BlockingHttpClient httpClient;

  @Inject
  public HttpClientFacetImpl(
      final HttpClientManager httpClientManager,
      final Map<String, AutoBlockConfiguration> autoBlockConfiguration,
      final Map<String, RedirectStrategy> redirectStrategy,
      final Map<String, NormalizationStrategy> normalizationStrategies,
      final Map<String, ContentCompressionStrategy> contentCompressionStrategies,
      final Map<String, TargetAuthenticationStrategy> authenticationStrategies)
  {
    this.httpClientManager = checkNotNull(httpClientManager);
    this.autoBlockConfiguration = checkNotNull(autoBlockConfiguration);
    this.redirectStrategy = checkNotNull(redirectStrategy);
    this.normalizationStrategies = checkNotNull(normalizationStrategies);
    this.contentCompressionStrategies = checkNotNull(contentCompressionStrategies);
    this.authenticationStrategies = checkNotNull(authenticationStrategies);
  }

  @VisibleForTesting
  HttpClientFacetImpl(
      final HttpClientManager httpClientManager,
      final Map<String, AutoBlockConfiguration> autoBlockConfiguration,
      final Map<String, RedirectStrategy> redirectStrategy,
      final Map<String, NormalizationStrategy> normalizationStrategy,
      final Map<String, ContentCompressionStrategy> contentCompressionStrategies,
      final Map<String, TargetAuthenticationStrategy> authenticationStrategies,
      final Config config)
  {
    this(httpClientManager, autoBlockConfiguration, redirectStrategy, normalizationStrategy,
        contentCompressionStrategies, authenticationStrategies);
    this.config = checkNotNull(config);
    checkNotNull(autoBlockConfiguration.get(DEFAULT));
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

  @Nullable
  @Override
  @Guarded(by = STARTED)
  public Header createBasicAuthHeader() {
    if (config.authentication instanceof UsernameAuthenticationConfiguration) {
      UsernameAuthenticationConfiguration userAuth = (UsernameAuthenticationConfiguration) config.authentication;

      if (unencryptedPassword == null) {
        unencryptedPassword = new String(userAuth.getPassword().decrypt());
      }

      String auth = format("%1$s:%2$s", userAuth.getUsername(), unencryptedPassword);

      byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(ISO_8859_1));

      String authHeader = "Basic " + new String(encodedAuth, ISO_8859_1);

      return new BasicHeader(AUTHORIZATION, authHeader);
    }
    else {
      log.debug("Basic auth header cannot be created for auth config of {}", config.authentication);
      return null;
    }
  }

  @Override
  public String getBearerToken() {
    if (config.authentication != null) {
      if (BearerTokenAuthenticationConfiguration.TYPE.equals(config.authentication.getType())) {
        return ((BearerTokenAuthenticationConfiguration) config.authentication).getBearerToken();
      } else if (GoogleAuthenticationConfiguration.TYPE.equals(config.authentication.getType())) {
        try {
          GoogleCredentials creds = GoogleCredentials.getApplicationDefault();
          creds.refreshIfExpired();
          return creds.getAccessToken().getTokenValue();
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        } 
      }
    }
    return null;
  }

  @Override
  @Guarded(by = STARTED)
  public RemoteConnectionStatus getStatus() {
    return httpClient.getStatus();
  }

  @Override
  @Guarded(by = STARTED)
  public void setStatus(final RemoteConnectionStatus status) {
    httpClient.setRemoteConnectionStatus(status);
  }

  @Subscribe
  public void on(final HttpClientConfigurationChangedEvent event) throws IOException {
    closeHttpClient();
    createHttpClient();
  }

  @Override
  public void onStatusChanged(final RemoteConnectionStatus oldStatus, final RemoteConnectionStatus newStatus) {
    logStatusChange(oldStatus, newStatus);
    getEventManager().post(new RemoteConnectionStatusEvent(newStatus, getRepository()));

    distributeRepositoryConnectionStatusChangedEvent(getRepository().getName(), newStatus);
  }

  private void logStatusChange(final RemoteConnectionStatus oldStatus, final RemoteConnectionStatus newStatus) {
    if (log.isInfoEnabled()) {
      if (oldStatus.getType() == AUTO_BLOCKED_UNAVAILABLE && newStatus.getType() == AUTO_BLOCKED_UNAVAILABLE) {
        logAutoBlockTimeIncreased(oldStatus, newStatus);
      }
      else if (oldStatus.getType() == UNINITIALISED) {
        log.info("Remote connection status of repository {} set to {}.", getRepository().getName(),
            newStatus.getDescription());
      }
      else {
        logStatusUpdated(oldStatus, newStatus);
      }
    }
  }

  private void logAutoBlockTimeIncreased(
      final RemoteConnectionStatus oldStatus,
      final RemoteConnectionStatus newStatus)
  {
    String message = "Repository status for {} continued as {} until {} - reason {} (previous reason was {})";
    log.info(message,
        getRepository().getName(),
        newStatus.getType(),
        newStatus.getBlockedUntil(),
        statusReason(newStatus),
        statusReason(oldStatus));
  }

  private void logStatusUpdated(final RemoteConnectionStatus oldStatus, final RemoteConnectionStatus newStatus) {
    String message = "Repository status for {} changed from {} to {}{} - reason {}";
    log.info(message,
        getRepository().getName(),
        oldStatus.getType(),
        newStatus.getType(),
        statusBlockedUntil(newStatus),
        statusReason(newStatus));
  }

  private static String statusBlockedUntil(final RemoteConnectionStatus status) {
    if (status.getBlockedUntil() != null) {
      return format(" until %s", status.getBlockedUntil());
    }
    return "";
  }

  private static String statusReason(final RemoteConnectionStatus status) {
    return format("%s for %s",
        status.getReason() != null ? status.getReason() : "n/a",
        status.getRequestUrl() != null ? status.getRequestUrl() : "n/a");
  }

  private void createHttpClient() {
    HttpClientConfiguration delegateConfig = getHttpClientConfiguration(httpClientManager, config);
    CloseableHttpClient delegate = httpClientManager.create(new ConfigurationCustomizer(delegateConfig));

    boolean online = getRepository().getConfiguration().isOnline();
    // wrap delegate with auto-block aware client
    httpClient = new BlockingHttpClient(delegate, config, this, online, getAutoBlockConfiguration());
    log.debug("Created HTTP client: {}", httpClient);
  }

  protected HttpClientConfiguration getHttpClientConfiguration(
      final HttpClientManager httpClientManager,
      final Config config)
  {
    // construct http client delegate
    HttpClientConfiguration delegateConfig = httpClientManager.newConfiguration();
    delegateConfig.setConnection(config.connection);
    delegateConfig.setAuthentication(config.authentication);
    delegateConfig.setRedirectStrategy(getRedirectStrategy());
    setNormalizationStrategy(delegateConfig);
    setContentCompressionStrategy(delegateConfig);
    setAuthenticationStrategy(delegateConfig);
    return delegateConfig;
  }

  private AutoBlockConfiguration getAutoBlockConfiguration() {
    AutoBlockConfiguration config = this.autoBlockConfiguration.get(getRepository().getFormat().getValue());

    if (config == null) {
      config = autoBlockConfiguration.get(DEFAULT);
    }

    return config;
  }

  protected RedirectStrategy getRedirectStrategy() {
    return this.redirectStrategy.get(getRepository().getFormat().getValue());
  }

  protected void setNormalizationStrategy(final HttpClientConfiguration delegateConfig) {
    ofNullable(normalizationStrategies.get(getRepository().getFormat().getValue()))
        .ifPresent(strategy -> delegateConfig.setNormalizeUri(strategy.shouldNormalizeUri()));
  }

  protected void setContentCompressionStrategy(final HttpClientConfiguration delegateConfig) {
    ofNullable(contentCompressionStrategies.get(getRepository().getFormat().getValue()))
        .ifPresent(strategy -> delegateConfig
            .setDisableContentCompression(strategy.shouldDisableContentCompression(getRepository().getName())));
  }

  private void setAuthenticationStrategy(final HttpClientConfiguration delegateConfig) {
    ofNullable(authenticationStrategies.get(getRepository().getFormat().getValue()))
        .ifPresent(delegateConfig::setAuthenticationStrategy);
  }

  private void closeHttpClient() throws IOException {
    log.debug("Closing HTTP client: {}", httpClient);
    httpClient.close();
    httpClient = null;
  }

  private void distributeRepositoryConnectionStatusChangedEvent(
      final String repositoryName,
      final RemoteConnectionStatus status)
  {
    if (EventHelper.isReplicating()) {
      log.debug("Originally triggered by remote event");
      return;
    }

    log.debug("Distribute repository block changed event: repository={}:{}", repositoryName, status.getType());

    long blockUntilMillis = status.getBlockedUntil() == null
        ? DateTime.now().getMillis()
        : status.getBlockedUntil().getMillis();

    RepositoryRemoteConnectionStatusEvent event = new RepositoryRemoteConnectionStatusEvent(
        repositoryName,
        status.getType().ordinal(),
        status.getReason(),
        blockUntilMillis,
        status.getRequestUrl());

    getEventManager().post(event);
  }
}
