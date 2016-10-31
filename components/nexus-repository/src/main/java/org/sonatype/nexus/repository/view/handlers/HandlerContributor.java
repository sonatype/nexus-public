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
package org.sonatype.nexus.repository.view.handlers;

import java.util.List;
import java.util.ListIterator;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.Router.LOCAL_ATTRIBUTE_PREFIX;

/**
 * A {@link Handler} which diverts request processing through any {@link ContributedHandler}s that are active.
 *
 * @since 3.1
 */
@Named
@Singleton
public class HandlerContributor
    implements Handler
{
  private final List<ContributedHandler> contributedHandlers;

  @VisibleForTesting
  static final String EXTENDED_MARKER =
      LOCAL_ATTRIBUTE_PREFIX + HandlerContributor.class.getName() + ".extended";

  @Inject
  public HandlerContributor(final List<ContributedHandler> contributedHandlers)
  {
    this.contributedHandlers = checkNotNull(contributedHandlers);
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    // Ensure the extra handlers are inserted only once, in the case that a handler higher
    // on the stack calls proceed() twice for some reason
    if (!isMarkedExtended(context)) {
      ListIterator<ContributedHandler> handlerIterator = contributedHandlers.listIterator(contributedHandlers.size());
      while (handlerIterator.hasPrevious()) {
        context.insertHandler(handlerIterator.previous());
      }
      markExtended(context);
    }

    return context.proceed();
  }

  private boolean isMarkedExtended(final Context context) {
    return context.getAttributes().contains(EXTENDED_MARKER);
  }

  private void markExtended(final Context context) {
    context.getAttributes().set(EXTENDED_MARKER, Boolean.TRUE);
  }
}
