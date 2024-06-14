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
package org.sonatype.nexus.repository;

import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.repository.recipe.RouterBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link Recipe} implementations.
 *
 * @since 3.0
 */
public abstract class RecipeSupport
    extends ComponentSupport
    implements Recipe
{
  private final Format format;

  private final Type type;

  private BrowseUnsupportedHandler browseUnsupportedHandler;

  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  private DatabaseCheck databaseCheck;

  protected RecipeSupport(final Type type, final Format format) {
    this.type = checkNotNull(type);
    this.format = checkNotNull(format);
  }

  @Override
  public Format getFormat() {
    return format;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "format=" + format +
        ", type=" + type +
        '}';
  }

  @Inject
  public void setHighAvailabilitySupportChecker(final HighAvailabilitySupportChecker highAvailabilitySupportChecker) {
    this.highAvailabilitySupportChecker = highAvailabilitySupportChecker;
  }

  @Inject
  public void setBrowseUnsupportedHandler(final BrowseUnsupportedHandler browseUnsupportedHandler) {
    this.browseUnsupportedHandler = checkNotNull(browseUnsupportedHandler);
  }

  @Inject
  public void setDatabaseCheck(final DatabaseCheck databaseCheck) {
    this.databaseCheck = databaseCheck;
  }

  /**
   * Adds route to redirect access directly with a browser to a handler with links to the repo's components and
   * assets.
   */
  protected void addBrowseUnsupportedRoute(RouterBuilder builder) {
    builder.route(browseUnsupportedHandler.getRoute());
  }

  @Override
  public boolean isFeatureEnabled() {
    if (databaseCheck != null && !databaseCheck.isAllowedByVersion(getClass())) {
      return false;
    }
    if (highAvailabilitySupportChecker != null) {
      return highAvailabilitySupportChecker.isSupported(getFormat().getValue());
    }
    log.error("HighAvailabilitySupportChecker not found - Format {} will not be enabled", getFormat());
    return false;
  }
}
