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
package org.sonatype.nexus.common.db;

public interface DatabaseCheck
{
  public static final String POSTGRE_SQL = "PostgreSQL";

  boolean isPostgresql();

  /**
   * To be used during startup to determine if minimum schema versions >= currently running schema
   * 
   * @param annotatedClass class to check for <code>@AvailabilityVersion(from = "1.0")</code>
   * @return true if the class is allowed to start based on current database schema
   */
  boolean isAllowedByVersion(Class<?> annotatedClass);

  /**
   * To be used at runtime, to execute code paths according to the schema version
   * 
   * @param version the version to verify against
   * @return true if the database schema version is >= than the argument
   */
  boolean isAtLeast(final String version);
}
