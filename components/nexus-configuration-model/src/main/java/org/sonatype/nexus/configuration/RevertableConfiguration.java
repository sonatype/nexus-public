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
 * Revertable configuration is a configuration that is changeable, but may be be reverted (rollback the changes). The
 * changes are "visible" only after applyChanges() call.
 *
 * @author cstamas
 */
public interface RevertableConfiguration<C>
{
  /**
   * Returns true if this configuration holds some changes that are not persisted.
   */
  boolean isDirty();

  /**
   * Validates the changes, if any.
   */
  void validateChanges()
      throws ConfigurationException;

  /**
   * Commits the changes. Resets the state of config "back to normal" (saved). Will call validateChanges() if needed.
   */
  void commitChanges()
      throws ConfigurationException;

  /**
   * Rollback the changes. Resets the state of config "back to normal" (saved).
   */
  void rollbackChanges();

  // ==

  C getConfiguration(boolean forWrite);
}
