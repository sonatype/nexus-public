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
package org.sonatype.nexus.cleanup.internal.content.search;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.store.ComponentSetData;

import java.util.Map;

public class ComponentSetCleanupBrowser
        extends ComponentSupport
        implements ContinuationBrowse<FluentComponent> {
  private final Repository repository;

  private final CleanupPolicy policy;

  private final boolean includeAssets;

  ComponentSetCleanupBrowser(Repository repository, CleanupPolicy policy, boolean includeAssets) {
    if (repository == null) {
      throw new IllegalArgumentException("Repository must not be null");
    }
    this.repository = repository;

    if (policy == null) {
      throw new IllegalArgumentException("Cleanup Policy must not be null");
    }
    this.policy = policy;

    this.includeAssets = includeAssets;
  }

  @Override
  public Continuation<FluentComponent> browse(final Integer limit, final String continuationToken) {

    if (limit < 1) {
      throw new IllegalArgumentException("Browse limit must be at least 1");
    }

    log.debug("Request for components to clean with a limit of {} and a token of {}", limit, continuationToken);
    log.debug("CleanupCriteria = {}", this.policy.getCriteria());

    FluentComponents fluentComponents = repository.facet(ContentFacet.class).components();

    int remainingLimit = limit;
    CompositeComponentSetContinuation<FluentComponent> compositeContinuation =
            new CompositeComponentSetContinuation<>(continuationToken);
    Map<String, String> criteria = this.policy.getCriteria();
    boolean selectBySet = criteria.containsKey("sortBy") || criteria.containsKey("retain");

    CleanupQueryResult processed;
    do {
      if (selectBySet) {
        processed = processComponentSets(remainingLimit, fluentComponents, compositeContinuation);
      }
      else {
        processed = processComponents(remainingLimit, null, fluentComponents,
                compositeContinuation);
      }
      remainingLimit = processed.remainingLimit();
      if (!processed.hasMore) {
        log.debug("No more component sets to process");
        compositeContinuation.setComponentSetContinuationToken(null);
        compositeContinuation.setComponentContinuationToken(null);
        break;
      }

    }
    while (remainingLimit > 0);

    if (log.isDebugEnabled()) {
      log.debug("Final result: items={}, remaining={}, setToken={}, componentToken={}, nextToken={}",
              compositeContinuation.size(),
              remainingLimit,
              compositeContinuation.getComponentSetContinuationToken(),
              compositeContinuation.getComponentContinuationToken(),
              compositeContinuation.nextContinuationToken());
    }
    return compositeContinuation;

  }

  CleanupQueryResult processComponentSets(final int limit,
                                          final FluentComponents fluentComponents,
                                          final CompositeComponentSetContinuation<FluentComponent> compositeContinuation) {

    Continuation<ComponentSetData> componentSets =
            fluentComponents.sets(limit, compositeContinuation.getComponentSetContinuationToken());
    log.debug("Retrieved {} component sets with a limit of {} and a continuation token of {}", componentSets.size(),
            limit, compositeContinuation.getComponentSetContinuationToken());

    if (componentSets.isEmpty()) {
      return new CleanupQueryResult(0, false);
    }

    int remainingLimit = limit;

    for (ComponentSetData componentSet : componentSets) {
      CleanupQueryResult result = processComponentSet(remainingLimit, componentSet, fluentComponents, compositeContinuation);
      remainingLimit = result.remainingLimit();
      if (remainingLimit <= 0) {
        break;
      }
    }

    return new CleanupQueryResult(remainingLimit, componentSets.size() >= limit || remainingLimit <= 0);
  }

  CleanupQueryResult processComponentSet(final int limit,
                                         final ComponentSetData componentSet,
                                         final FluentComponents fluentComponents,
                                         final CompositeComponentSetContinuation<FluentComponent> compositeContinuation) {
    int remainingLimit = limit;

    if (remainingLimit <= 0) {
      return new CleanupQueryResult(remainingLimit, true);
    }

    if (log.isDebugEnabled()) {
      log.debug("Processing {}", componentSet.toStringExternal());
    }

    do {
      CleanupQueryResult processed = processComponents(remainingLimit, componentSet, fluentComponents, compositeContinuation);
      remainingLimit = processed.remainingLimit();

      if (!processed.hasMore()) {
        // No more components - next set
        if (log.isDebugEnabled()) {
          log.debug("Updating set token to {} and clearing component token", componentSet.nextContinuationToken());
        }
        compositeContinuation.setComponentSetContinuationToken(componentSet.nextContinuationToken());
        compositeContinuation.setComponentContinuationToken(null);
        break;
      }

    }
    while (remainingLimit > 0);

    return new CleanupQueryResult(remainingLimit, remainingLimit <= 0);
  }

  CleanupQueryResult processComponents(final int limit,
                                       final ComponentSetData componentSet,
                                       final FluentComponents fluentComponents,
                                       final CompositeComponentSetContinuation<FluentComponent> compositeContinuation) {

    String componentToken = compositeContinuation.getComponentContinuationToken();

    Continuation<FluentComponent> components =
            fluentComponents.byCleanupCriteria(componentSet, this.policy.getCriteria(), includeAssets, limit, componentToken);

    if (log.isDebugEnabled()) {
      log.debug("Retrieved {} components for the set {} with a limit of {} and a token of {}", components.size(),
              componentSet == null ? null : componentSet.toStringExternal(),
              limit, componentToken);
    }

    if (components.isEmpty()) {
      log.debug("No components found.");
      compositeContinuation.setComponentContinuationToken(null);
      return new CleanupQueryResult(limit, false);
    }

    compositeContinuation.addAll(components);
    String nextComponentToken = components.nextContinuationToken();
    log.debug("Updating component token to {}", nextComponentToken);
    compositeContinuation.setComponentContinuationToken(nextComponentToken);

    int retrieved = components.size();
    int safeRemainingLimit = Math.max(0, limit - retrieved);
    return new CleanupQueryResult(safeRemainingLimit, retrieved >= limit && nextComponentToken != null);
  }

  private static final class CleanupQueryResult {

    private final int remainingLimit;
    private final boolean hasMore;

    CleanupQueryResult(final int remainingLimit, final boolean hasMore) {
      this.remainingLimit = remainingLimit;
      this.hasMore = hasMore;
    }

    int remainingLimit() {
      return this.remainingLimit;
    }

    boolean hasMore() {
      return this.hasMore;
    }
  }
}
