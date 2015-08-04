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
package org.sonatype.nexus.configuration;

import org.sonatype.configuration.ConfigurationException;


/**
 * A Configurable component.
 *
 * @author cstamas
 */
public interface Configurable<C>
{
  /**
   * Returns the current core configuration of the component.May return null if there is not config object set.
   */
  CoreConfiguration<C> getCurrentCoreConfiguration();

  /**
   * Sets the configuration object and calls configure(). A shortcut for setCurrentConfiguration(config) and then
   * configure() calls.
   */
  void configure(Object config)
      throws ConfigurationException;

  /**
   * Returns true if there are some unsaved changes.
   */
  boolean isDirty();

  /**
   * Commits the changes. Resets the state of config "back to normal" (saved).
   */
  boolean commitChanges()
      throws ConfigurationException;

  /**
   * Rollbacks the changes. Resets the state of config "back to normal" (saved).
   */
  boolean rollbackChanges();

  /**
   * A simple short name.
   */
  String getName();
}
