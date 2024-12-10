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
package org.sonatype.nexus.repository.rest.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAuthenticationAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.ReplicationAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import com.google.common.collect.Sets;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTPS;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STRICT_CONTENT_TYPE_VALIDATION;

/**
 * @since 3.20
 */
@Named
public class ProxyRepositoryApiRequestToConfigurationConverter<T extends ProxyRepositoryApiRequest>
    extends AbstractRepositoryApiRequestToConfigurationConverter<T>
{
  private final RoutingRuleStore routingRuleStore;

  @Inject
  public ProxyRepositoryApiRequestToConfigurationConverter(final RoutingRuleStore routingRuleStore) {
    this.routingRuleStore = routingRuleStore;
  }

  @Override
  public Configuration convert(final T request) {
    Configuration configuration = super.convert(request);
    convertRoutingRule(request, configuration);
    convertStorage(request, configuration);
    convertCleanup(request, configuration);
    convertProxy(request, configuration);
    convertNegativeCache(request, configuration);
    convertHttpClient(request, configuration);
    convertReplication(request, configuration);
    return configuration;
  }

  private void convertHttpClient(final T request, final Configuration configuration) {
    HttpClientAttributes httpClient = request.getHttpClient();
    if (nonNull(httpClient)) {
      NestedAttributesMap httpClientConfiguration = configuration.attributes("httpclient");
      httpClientConfiguration.set("blocked", httpClient.getBlocked());
      httpClientConfiguration.set("autoBlock", httpClient.getAutoBlock());
      HttpClientConnectionAttributes connection = httpClient.getConnection();
      NestedAttributesMap connectionConfiguration = httpClientConfiguration.child("connection");
      convertConnection(connection, connectionConfiguration, request.getProxy().getRemoteUrl().startsWith(HTTPS));
      convertAuthentication(httpClient, httpClientConfiguration);
    }
  }

  private void convertAuthentication(
      final HttpClientAttributes httpClient,
      final NestedAttributesMap httpClientConfiguration)
  {
    HttpClientConnectionAuthenticationAttributes authentication = httpClient.getAuthentication();
    if (nonNull(authentication)) {
      NestedAttributesMap authenticationConfiguration = httpClientConfiguration.child("authentication");
      authenticationConfiguration.set("type", authentication.getType());
      authenticationConfiguration.set("username", authentication.getUsername());
      authenticationConfiguration.set("password", authentication.getPassword());
      authenticationConfiguration.set("ntlmHost", authentication.getNtlmHost());
      authenticationConfiguration.set("ntlmDomain", authentication.getNtlmDomain());
      authenticationConfiguration.set("bearerToken", authentication.getBearerToken());
    }
  }

  private void convertConnection(
      final HttpClientConnectionAttributes connection,
      final NestedAttributesMap connectionConfiguration,
      final boolean isHttps)
  {
    if (nonNull(connection)) {
      if (Boolean.TRUE.equals(connection.getUseTrustStore()) && !isHttps) {
        throw new IllegalArgumentException("TrustStore is available only with HTTPS remote URLs");
      }

      connectionConfiguration.set("retries", connection.getRetries());
      connectionConfiguration.set("userAgentSuffix", connection.getUserAgentSuffix());
      connectionConfiguration.set("timeout", connection.getTimeout());
      connectionConfiguration.set("enableCircularRedirects", connection.getEnableCircularRedirects());
      connectionConfiguration.set("enableCookies", connection.getEnableCookies());
      connectionConfiguration.set("useTrustStore", connection.getUseTrustStore());
    }
  }

  private void convertNegativeCache(final T request, final Configuration configuration) {
    NegativeCacheAttributes negativeCache = request.getNegativeCache();
    if (nonNull(negativeCache)) {
      NestedAttributesMap negativeCacheConfiguration = configuration.attributes("negativeCache");
      negativeCacheConfiguration.set("enabled", negativeCache.getEnabled());
      negativeCacheConfiguration.set("timeToLive", negativeCache.getTimeToLive());
    }
  }

  private void convertProxy(final T request, final Configuration configuration) {
    ProxyAttributes proxy = request.getProxy();
    if (nonNull(proxy)) {
      NestedAttributesMap proxyConfiguration = configuration.attributes("proxy");
      proxyConfiguration.set("remoteUrl", proxy.getRemoteUrl());
      proxyConfiguration.set("contentMaxAge", proxy.getContentMaxAge());
      proxyConfiguration.set("metadataMaxAge", proxy.getMetadataMaxAge());
    }
  }

  private void convertCleanup(final T request, final Configuration configuration) {
    CleanupPolicyAttributes cleanup = request.getCleanup();
    if (nonNull(cleanup)) {
      NestedAttributesMap cleanupConfiguration = configuration.attributes("cleanup");
      cleanupConfiguration.set("policyName", Sets.newHashSet(cleanup.getPolicyNames()));
    }
  }

  private void convertStorage(final T request, final Configuration configuration) {
    StorageAttributes storage = request.getStorage();
    if (nonNull(storage)) {
      NestedAttributesMap storageConfiguration = configuration.attributes(STORAGE);
      storageConfiguration.set(BLOB_STORE_NAME, storage.getBlobStoreName());
      storageConfiguration.set(STRICT_CONTENT_TYPE_VALIDATION, storage.getStrictContentTypeValidation());
      maybeAddDataStoreName(configuration);
    }
  }

  private void convertRoutingRule(final T request, final Configuration configuration) {
    String routingRuleName = request.getRoutingRule();
    if (!Strings2.isBlank(routingRuleName)) {
      RoutingRule routingRule = routingRuleStore.getByName(routingRuleName);
      if (nonNull(routingRule)) {
        configuration.setRoutingRuleId(routingRule.id());
      }
    }
  }

  private void convertReplication(final T request, final Configuration configuration) {
    ReplicationAttributes replication = request.getReplication();
    if (nonNull(replication)) {
      NestedAttributesMap replicationConfiguration = configuration.attributes("replication");
      replicationConfiguration.set("preemptivePullEnabled", replication.getPreemptivePullEnabled());
      replicationConfiguration.set("assetPathRegex", replication.getAssetPathRegex());
    }
  }
}
