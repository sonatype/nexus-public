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
package org.sonatype.nexus.common.upgrade.events;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.event.EventWithSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support class for the database migration events
 */
public abstract class UpgradeEventSupport
    extends EventWithSource
{
  private String user;

  private String schemaVersion;

  private String[] migrations;

  protected UpgradeEventSupport() {
    // deserialization
  }

  protected UpgradeEventSupport(
      @Nullable final String user,
      @Nullable final String schemaVersion,
      final String[] migrations)
  {
    this.user = user;
    this.schemaVersion = schemaVersion;
    this.migrations = checkNotNull(migrations);
  }

  /**
   * @return the migrations associated with the event
   */
  public String[] getMigrations() {
    return migrations;
  }

  /**
   * @return the database schema version at the event time, may be null if this is the first system start
   */
  public Optional<String> getSchemaVersion() {
    return Optional.ofNullable(schemaVersion);
  }

  /**
   * @return the userId of the person who triggered it, or empty if this was triggered automatically
   */
  public Optional<String> getUser() {
    return Optional.ofNullable(user);
  }

  public void setMigrations(final String[] migrations) {
    this.migrations = migrations;
  }

  public void setSchemaVersion(final String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public void setUser(final String user) {
    this.user = user;
  }
}
