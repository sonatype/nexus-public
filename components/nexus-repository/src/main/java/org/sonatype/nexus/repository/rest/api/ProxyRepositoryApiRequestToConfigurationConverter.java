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

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.ProxyRepositoryApiRequest;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import com.google.common.collect.Sets;

import static org.sonatype.nexus.repository.storage.StorageFacetConstants.BLOB_STORE_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STORAGE;
import static org.sonatype.nexus.repository.storage.StorageFacetConstants.STRICT_CONTENT_TYPE_VALIDATION;

/**
 * @since 3.next
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

  public Configuration convert(final T request) {
    Configuration configuration = super.convert(request);
    String routingRuleName = request.getRoutingRule();
    if (!Strings2.isBlank(routingRuleName)) {
      RoutingRule routingRule = routingRuleStore.getByName(routingRuleName);
      if (routingRule != null) {
        configuration.attributes("routingRuleId")
            .set("routingRuleId", routingRule.getEntityMetadata().getId().getValue());
      }
    }
    configuration.attributes(STORAGE).set(BLOB_STORE_NAME, request.getStorage().getBlobStoreName());
    configuration.attributes(STORAGE)
        .set(STRICT_CONTENT_TYPE_VALIDATION, request.getStorage().getStrictContentTypeValidation());
    configuration.attributes("cleanup")
        .set("policyName",
            request.getCleanup() != null ? Sets.newHashSet(request.getCleanup().getPolicyNames()) : null);
    configuration.attributes("proxy").set("remoteUrl", request.getProxy().getRemoteUrl());
    configuration.attributes("proxy").set("contentMaxAge", request.getProxy().getContentMaxAge());
    configuration.attributes("proxy").set("metadataMaxAge", request.getProxy().getMetadataMaxAge());
    configuration.attributes("negativeCache").set("enabled", request.getNegativeCache().getEnabled());
    configuration.attributes("negativeCache").set("timeToLive", request.getNegativeCache().getTimeToLive());
    configuration.attributes("httpclient").set("blocked", request.getHttpClient().getBlocked());
    configuration.attributes("httpclient").set("autoBlock", request.getHttpClient().getAutoBlock());
    configuration.attributes("httpclient").child("connection")
        .set("retries", request.getHttpClient().getConnection().getRetries());
    configuration.attributes("httpclient").child("connection")
        .set("userAgentSuffix", request.getHttpClient().getConnection().getUserAgentSuffix());
    configuration.attributes("httpclient").child("connection")
        .set("timeout", request.getHttpClient().getConnection().getTimeout());
    configuration.attributes("httpclient").child("connection")
        .set("enableCircularRedirects", request.getHttpClient().getConnection().getEnableCircularRedirects());
    configuration.attributes("httpclient").child("connection")
        .set("enableCookies", request.getHttpClient().getConnection().getEnableCookies());
    configuration.attributes("httpclient").child("connection").child("authentication")
        .set("type", request.getHttpClient().getAuthentication().getType());
    configuration.attributes("httpclient").child("connection").child("authentication")
        .set("username", request.getHttpClient().getAuthentication().getUsername());
    configuration.attributes("httpclient").child("connection").child("authentication")
        .set("password", request.getHttpClient().getAuthentication().getPassword());
    configuration.attributes("httpclient").child("connection").child("authentication")
        .set("ntlmHost", request.getHttpClient().getAuthentication().getNtlmHost());
    configuration.attributes("httpclient").child("connection").child("authentication")
        .set("ntlmDomain", request.getHttpClient().getAuthentication().getNtlmDomain());
    return configuration;
  }
}
