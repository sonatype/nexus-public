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
package org.sonatype.nexus.repository.raw;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE;

/**
 * Adapter to expose raw specific repository configuration for the repositories REST API.
 *
 * @since 3.41
 */
@Component
@Qualifier(RawFormat.NAME)
public class RawRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  private static final String RAW = "raw";

  private final boolean rawQueryParamsForwardingEnabled;

  @Autowired
  public RawRepositoryAdapter(
      final RoutingRuleStore routingRuleStore,
      @Value(RAW_QUERYPARAMS_FORWARDING_ENABLED_NAMED_VALUE) final boolean rawQueryParamsForwardingEnabled)
  {
    super(routingRuleStore);
    this.rawQueryParamsForwardingEnabled = rawQueryParamsForwardingEnabled;
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    return switch (repository.getType().toString()) {
      case HostedType.NAME -> new RawHostedApiRepository(
          repository.getName(),
          repository.getUrl(),
          repository.getConfiguration().isOnline(),
          getHostedStorageAttributes(repository),
          getCleanupPolicyAttributes(repository),
          getComponentAttributes(repository),
          createRawAttributes(repository));
      case ProxyType.NAME -> new RawProxyApiRepository(
          repository.getName(),
          repository.getUrl(),
          repository.getConfiguration().isOnline(),
          getHostedStorageAttributes(repository),
          getCleanupPolicyAttributes(repository),
          getProxyAttributes(repository),
          getNegativeCacheAttributes(repository),
          getHttpClientAttributes(repository),
          getRoutingRuleName(repository),
          getReplicationAttributes(repository),
          createRawAttributes(repository));
      case GroupType.NAME -> new RawGroupApiRepository(
          repository.getName(),
          repository.getUrl(),
          repository.getConfiguration().isOnline(),
          getHostedStorageAttributes(repository),
          getGroupAttributes(repository),
          createRawAttributes(repository));
      default -> throw new IllegalArgumentException("Unsupported repository type: " + repository.getType());
    };
  }

  @SuppressWarnings("unchecked")
  private RawAttributes createRawAttributes(final Repository repository) {
    NestedAttributesMap rawAttributes = repository.getConfiguration().attributes(RAW);

    // Normalize null to INLINE - legacy repos with null serve inline at handler level
    String disposition = Optional.ofNullable(rawAttributes.get("contentDisposition", String.class))
        .orElse(ContentDisposition.INLINE.name());

    if (!rawQueryParamsForwardingEnabled) {
      return new RawAttributes(disposition);
    }
    Boolean forwardQueryParameters = rawAttributes.get("forwardQueryParameters", Boolean.class);
    List<String> excludedQueryParameters =
        rawAttributes.get("excludedQueryParameters", List.class, Collections.emptyList());
    return new RawAttributes(disposition, forwardQueryParameters, excludedQueryParameters);
  }
}
