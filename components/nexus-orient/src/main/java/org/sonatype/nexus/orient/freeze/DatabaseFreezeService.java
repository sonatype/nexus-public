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
package org.sonatype.nexus.orient.freeze;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;

/**
 * Service that freezes and releases all OrientDB databases.
 *
 * @since 3.2
 */
public interface DatabaseFreezeService
{
  /**
   * Freeze all databases. Will emit an event.
   */
  void freezeAllDatabases();

  /**
   * Release all databases. Will emit an event.
   */
  void releaseAllDatabases();

  /**
   * Returns whether databases are currently frozen.
   */
  boolean isFrozen();

  /**
   * Check if the database is frozen and throw a {@link OModificationOperationProhibitedException} if it is.
   *
   * @throws OModificationOperationProhibitedException thrown if database is frozen
   */
  void checkUnfrozen();

  /**
   * Check if the database is frozen and throw a {@link OModificationOperationProhibitedException} if it is.
   *
   * @param message Message used when constructing the  OModificationOperationProhibitedException
   * @throws OModificationOperationProhibitedException thrown if database is frozen
   */
  void checkUnfrozen(String message);
}
