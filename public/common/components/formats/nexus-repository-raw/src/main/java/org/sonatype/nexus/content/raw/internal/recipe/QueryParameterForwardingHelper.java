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
package org.sonatype.nexus.content.raw.internal.recipe;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.BadRequestException;
import org.sonatype.nexus.repository.view.Parameters;

import com.google.common.collect.ImmutableSet;

/**
 * Builds query strings from request parameters with configurable forwarding and exclusion.
 * Manages its own thread-safe configuration state via an internal {@link AtomicReference}.
 *
 * <p>
 * Not a Spring bean -- created once per facet instance. Configuration is updated
 * atomically via {@link #updateConfig} when the repository configuration changes.
 */
final class QueryParameterForwardingHelper
{
  private static final Logger log = LoggerFactory.getLogger(QueryParameterForwardingHelper.class);

  /**
   * Immutable configuration snapshot for query parameter forwarding.
   */
  private record Config(boolean forwardQueryParameters, ImmutableSet<String> excludedParamsLowercase)
  {
  }

  @VisibleForTesting
  static final int MAX_FORWARDED_PARAMS = 50;

  private static final Config DISABLED = new Config(false, ImmutableSet.of());

  private final AtomicReference<Config> config = new AtomicReference<>(DISABLED);

  /**
   * Updates the forwarding configuration atomically.
   * Called from facet lifecycle methods ({@code doConfigure}/{@code doUpdate}).
   *
   * @param forward whether to forward query parameters to the remote
   * @param excludedParams parameter names to exclude (compared case-insensitively);
   *          null elements are filtered out
   */
  void updateConfig(final boolean forward, final Collection<String> excludedParams) {
    if (!forward) {
      log.debug("Query parameter forwarding disabled");
      config.set(DISABLED);
      return;
    }

    Collection<String> params = excludedParams != null ? excludedParams : Collections.emptyList();
    ImmutableSet<String> excludedLower = params.stream()
        .filter(Strings2::notBlank)
        .map(String::toLowerCase)
        .collect(ImmutableSet.toImmutableSet());

    config.set(new Config(true, excludedLower));
    log.debug("Query parameter forwarding enabled, excluded parameters: {}", excludedLower);
  }

  /**
   * Builds a query string from the request parameters.
   * Returns empty string when forwarding is disabled, parameters are null/empty,
   * or all parameters are excluded.
   *
   * <p>
   * Parameters are sorted alphabetically for deterministic cache keys.
   * Both keys and values are URL-encoded using {@link URLEncoder} ({@code application/x-www-form-urlencoded}),
   * which encodes spaces as {@code +} rather than {@code %20}. Most HTTP servers treat both
   * identically in query strings.
   *
   * <p>
   * <b>Note:</b> The {@link #MAX_FORWARDED_PARAMS} limit is enforced against the total number
   * of incoming parameters <i>before</i> exclusions are applied. A request that exceeds the limit
   * will be rejected with a {@link BadRequestException} even if the excluded parameters would
   * bring the effective count below the threshold.
   */
  String buildQueryString(final Parameters parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "";
    }

    Config snapshot = config.get();
    if (!snapshot.forwardQueryParameters()) {
      return "";
    }

    long totalParamCount = parameters.names()
        .stream()
        .mapToLong(key -> parameters.getAll(key).size())
        .sum();

    if (totalParamCount > MAX_FORWARDED_PARAMS) {
      throw new BadRequestException(
          String.format("Request contains %d query parameters, exceeding the maximum of %d",
              totalParamCount, MAX_FORWARDED_PARAMS));
    }

    StringBuilder queryString = new StringBuilder();
    boolean first = true;

    for (String key : new TreeSet<>(parameters.names())) {
      if (snapshot.excludedParamsLowercase().contains(key.toLowerCase())) {
        log.debug("Excluding query parameter '{}' from forwarding", key);
        continue;
      }

      for (String value : parameters.getAll(key)) {
        if (!first) {
          queryString.append("&");
        }
        first = false;

        queryString.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
        if (value != null) {
          queryString.append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
      }
    }

    return queryString.toString();
  }

  /**
   * Returns the current set of excluded parameter names (lowercased).
   * Package-private for test assertions.
   */
  ImmutableSet<String> getExcludedParamsLowercase() {
    return config.get().excludedParamsLowercase();
  }
}
