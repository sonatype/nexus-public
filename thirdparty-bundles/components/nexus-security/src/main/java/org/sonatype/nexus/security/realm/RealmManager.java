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
package org.sonatype.nexus.security.realm;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * Realm manager.
 *
 * @since 3.0
 */
public interface RealmManager
  extends Lifecycle
{
  /**
   * Returns copy of current realm configuration.
   */
  RealmConfiguration getConfiguration();

  /**
   * Installs new realm configuration.
   */
  void setConfiguration(RealmConfiguration configuration);

  /**
   * Check if given realm-name is enabled.
   */
  boolean isRealmEnabled(String realmName);

  /**
   * Helper to enable or disable given realm-name.
   *
   * @see #enableRealm
   * @see #disableRealm
   */
  void enableRealm(String realmName, boolean enable);

  /**
   * Enable given realm-name, if not already enabled.
   */
  void enableRealm(String realmName);

  /**
   * Disable given realm-name, if not already disabled.
   */
  void disableRealm(String realmName);
}
