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
package org.sonatype.nexus.orient;

import java.util.Map;

import org.sonatype.nexus.orient.quorum.DatabaseQuorumStatus;

/**
 * Service for maintenance and repair of Orient databases.
 *
 * @since 3.4
 */
public interface DatabaseMaintenanceService
{
  /**
   * @return a {@link DatabaseQuorumStatus} reflecting current state (never null)
   */
  DatabaseQuorumStatus getQuorumStatus();

  /**
   * @return a {@link DatabaseQuorumStatus} reflecting the current state of the specified database
   */
  DatabaseQuorumStatus getQuorumStatus(String databaseName);

  /**
   * attempt to force the cluster to accept writes on this node
   */
  void resetWriteQuorum();

  /**
   * Logs the current orientdb server status
   */
  void logServersStatus();

  /**
   * Logs the current orientdb server status
   */
  String fullServerStatus();

  /**
   * Retrieve the current database profiler statistics.
   */
  Map<String, Object> profilerStatistics();

  /**
   * Get the current role of the named database.
   *
   * @since 3.next
   */
  String getDatabaseRole(String databaseName);

  /**
   * Reset the named database to the given role.
   *
   * @since 3.next
   */
  void setDatabaseRole(String databaseName, String role);

  /**
   * Get the current status of the named database.
   *
   * @since 3.next
   */
  String getDatabaseStatus(String databaseName);

  /**
   * Reset the named database to the given status.
   *
   * @since 3.next
   */
  void setDatabaseStatus(String databaseName, String status);

  /**
   * Checks the named database; returns short summary in map form.
   *
   * @since 3.next
   */
  Map<String, Object> checkDatabase(String databaseName);

  /**
   * Attempts to repair the named database; returns short summary in map form.
   *
   * @since 3.next
   */
  Map<String, Object> repairDatabase(String databaseName);

  /**
   * Requests reinstallation of the named database with content from the cluster; returns short summary in map form.
   *
   * @since 3.next
   */
  Map<String, Object> reinstallDatabase(String databaseName);
}
