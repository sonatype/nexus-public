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

public class ComponentSetCleanupBrowser
    extends ComponentSupport
    implements ContinuationBrowse<FluentComponent>
{
  private final Repository repository;

  private final CleanupPolicy policy;

  ComponentSetCleanupBrowser(Repository repository, CleanupPolicy policy) {
    if (repository == null) {
      throw new IllegalArgumentException("Repository must not be null");
    }
    this.repository = repository;

    if (policy == null) {
      throw new IllegalArgumentException("Cleanup Policy must not be null");
    }
    this.policy = policy;
  }

  @Override
  public Continuation<FluentComponent> browse(final Integer limit, final String continuationToken) {
    log.debug("Request for components to clean with a limit of {} and a token of {}", limit, continuationToken);
    FluentComponents fluentComponents = repository.facet(ContentFacet.class).components();
    CompositeComponentSetContinuation<FluentComponent> compositeContinuation =
        new CompositeComponentSetContinuation<>(continuationToken);

    // retrieve 'limit' sets, continuing if necessary from the previous point
    Continuation<ComponentSetData> componentSets =
        fluentComponents.sets(limit, compositeContinuation.getComponentSetContinuationToken());
    log.debug("Retrieved {} component sets with a limit of {} and a continuation token of {}", componentSets.size(),
        limit, compositeContinuation.getComponentSetContinuationToken());

    int subLimit = limit;
    String subToken = compositeContinuation.getComponentContinuationToken();

    if (!componentSets.isEmpty()) {
      // for each set, retrieve the components up until the 'limit'.
      for (ComponentSetData componentSet : componentSets) {

        // TODO: this should be set to the retain value from the criteria when applicable NEXUS-39582
        int retainOffset = 0;
        log.debug("Processing {} with retain offset of {}", componentSet.toStringExternal(), retainOffset);

        while (subLimit > 0) {
          log.info("passing cleanupCriteria of {}", this.policy.getCriteria());
          Continuation<FluentComponent> components =
              fluentComponents.byCleanupCriteria(componentSet, this.policy.getCriteria(), subLimit, subToken);

          // TODO: Resetting the offset for the rest of this set should be fine. However, there could be a condition
          // TODO:  where the retainOffset is greater than the subLimit, in which case... things will go wrong.
          retainOffset = 0;

          log.debug("  Retrieved {} components for the set with a limit of {} and a token of {}", components.size(),
              subLimit, subToken);
          if (components.isEmpty()) {
            // This continuation of components is empty, so clear the token
            subToken = null;
            break;
          }
          subLimit -= components.size();
          compositeContinuation.addAll(components);

          // Move the continuation token to the next 'page' of components
          subToken = components.nextContinuationToken();
        }

        // ensure the new continuation token represents the correct component
        log.debug("  Updating component token to {}", subToken);
        compositeContinuation.setComponentContinuationToken(subToken);

        // If we've run out of components, also set the token to the next set
        if (subToken == null) {
          log.debug("  Updating set token to {}", componentSet.nextContinuationToken());
          compositeContinuation.setComponentSetContinuationToken(componentSet.nextContinuationToken());
        }
        else {
          log.debug("  Left set token unchanged at {}", compositeContinuation.getComponentSetContinuationToken());
        }
        if (subLimit <= 0) {
          break;
        }
      }
      log.debug("Final result: items={}, subLimit={}, setToken={}, componentToken={}", compositeContinuation.size(),
          subLimit, compositeContinuation.getComponentSetContinuationToken(),
          compositeContinuation.getComponentContinuationToken());
      return compositeContinuation;
    }
    log.debug("Returning empty");
    return new CompositeComponentSetContinuation<>();
  }
}
