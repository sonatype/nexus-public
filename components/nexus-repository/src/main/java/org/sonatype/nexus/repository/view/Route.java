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
package org.sonatype.nexus.repository.view;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * View route.
 *
 * @since 3.0
 */
public class Route
{
  private final Matcher matcher;

  private final List<Handler> handlers;

  public Route(final Matcher matcher, final List<Handler> handlers) {
    this.matcher = checkNotNull(matcher, "Missing matcher");
    checkNotNull(handlers, "Missing handlers");
    checkArgument(!handlers.isEmpty(), "At least one handler is required");
    this.handlers = handlers;
  }

  @Nonnull
  public Matcher getMatcher() {
    return matcher;
  }

  @Nonnull
  public List<Handler> getHandlers() {
    return handlers;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "matcher=" + matcher +
        ", handlers=" + handlers +
        '}';
  }

  //
  // Builder
  //

  /**
   * View {@link Route} builder.
   */
  public static class Builder
  {
    private Matcher matcher;

    private List<Handler> handlers = Lists.newArrayList();

    public Builder matcher(final Matcher matcher) {
      checkState(this.matcher == null, "Only one matcher allowed");
      this.matcher = checkNotNull(matcher);
      return this;
    }

    public Builder handler(final Handler handler) {
      checkNotNull(handler);
      handlers.add(handler);
      return this;
    }

    public Route create() {
      return new Route(matcher, handlers);
    }
  }
}
