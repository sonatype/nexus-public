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
package org.sonatype.nexus.repository.raw.rest;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.ContentDispositionHelper;
import org.sonatype.nexus.repository.rest.api.ProxyRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.repository.raw.rest.RawAttributes.CONTENT_DISPOSITION;
import static org.sonatype.nexus.repository.raw.rest.RawAttributes.EXCLUDED_QUERY_PARAMETERS;
import static org.sonatype.nexus.repository.raw.rest.RawAttributes.FORWARD_QUERY_PARAMETERS;

/**
 * @since 3.25
 */
@Component
public class RawProxyRepositoryApiRequestToConfigurationConverter
    extends ProxyRepositoryApiRequestToConfigurationConverter<RawProxyRepositoryApiRequest>
{
  private final boolean rawQueryParamsForwardingEnabled;

  private final RepositoryManager repositoryManager;

  @Autowired
  public RawProxyRepositoryApiRequestToConfigurationConverter(
      final RoutingRuleStore routingRuleStore,
      final RepositoryManager repositoryManager,
      @Value(RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE) final boolean rawQueryParamsForwardingEnabled)
  {
    super(routingRuleStore);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.rawQueryParamsForwardingEnabled = rawQueryParamsForwardingEnabled;
  }

  @Override
  public Configuration convert(final RawProxyRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    NestedAttributesMap configAttributes = configuration.attributes("raw");
    RawAttributes requestAttributes = request.getRaw();

    String requestedDisposition = null;
    if (requestAttributes != null && requestAttributes.getContentDisposition() != null) {
      requestedDisposition = request.getRaw().getContentDisposition().name();
    }

    // Always resolve content disposition , by default to ATTACHMENT on creation
    String contentDisposition = ContentDispositionHelper.resolveContentDisposition(
        requestedDisposition,
        repositoryManager,
        request.getName(),
        "raw");

    configAttributes.set(CONTENT_DISPOSITION, contentDisposition);

    if (requestAttributes != null && rawQueryParamsForwardingEnabled) {
      if (requestAttributes.getForwardQueryParameters() != null) {
        configAttributes.set(FORWARD_QUERY_PARAMETERS, requestAttributes.getForwardQueryParameters());
      }
      if (requestAttributes.getExcludedQueryParameters() != null) {
        configAttributes.set(EXCLUDED_QUERY_PARAMETERS, requestAttributes.getExcludedQueryParameters());
      }
    }

    return configuration;
  }
}
