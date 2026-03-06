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
package org.sonatype.nexus.repository.security;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Security handler that ensures the requesting user has permission to access repository content.
 * <p>
 * Creates and manages a request-scoped cache for selector evaluations to improve performance
 * for Docker digest pulls and other scenarios that cause repeated selector evaluations (NEXUS-50181).
 * </p>
 *
 * @since 3.0
 */
@Component
@Singleton
public class SecurityHandler
    extends ComponentSupport
    implements org.sonatype.nexus.repository.view.handlers.SecurityHandler
{
  @VisibleForTesting
  static final String AUTHORIZED_KEY = "security.authorized";

  /**
   * Key for storing the request-scoped selector evaluation cache.
   */
  @VisibleForTesting
  static final String SELECTOR_CACHE_KEY = "security.selectorCache";

  private final Handler loginsCounterHandler;

  @Inject
  public SecurityHandler(
      @Qualifier("nexus.analytics.loginsCounterHandler") @Nullable final Handler loginsCounterHandler)
  {
    this.loginsCounterHandler = loginsCounterHandler;
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    SecurityFacet securityFacet = context.getRepository().facet(SecurityFacet.class);

    // Create request-scoped cache for selector evaluations
    SelectorEvaluationCache selectorCache = new SelectorEvaluationCache();
    context.getAttributes().set(SELECTOR_CACHE_KEY, selectorCache);

    try {
      // we employ the model that one security check per request is all that is necessary, if this handler is in a
      // nested repository (because this is a group repository), there is no need to check authz again
      if (context.getAttributes().get(AUTHORIZED_KEY) == null) {
        securityFacet.ensurePermitted(context.getRequest(), selectorCache);
        context.getAttributes().set(AUTHORIZED_KEY, true);
        if (loginsCounterHandler != null) {
          context.insertHandler(loginsCounterHandler);
        }
      }

      return context.proceed();
    }
    finally {
      // Clear request-scoped selector evaluation cache to prevent memory leaks
      selectorCache.clear();
      context.getAttributes().remove(SELECTOR_CACHE_KEY);
    }
  }
}
