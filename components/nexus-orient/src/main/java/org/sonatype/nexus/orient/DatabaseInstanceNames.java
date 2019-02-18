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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Holds the names of commonly used databases.
 * 
 * @since 3.1
 */
public class DatabaseInstanceNames
{
  /**
   * Name of the database storing component metadata.
   */
  public static final String COMPONENT = "component";

  /**
   * Name of the database storing system configuration.
   */
  public static final String CONFIG = "config";

  /**
   * Name of the database storing security configuration.
   */
  public static final String SECURITY = "security";

  /**
   * Name of the database storing access log data for licensing.
   */
  public static final String ACCESSLOG = "accesslog";

  public static final Set<String> DATABASE_NAMES = ImmutableSet.of(
      ACCESSLOG, CONFIG, COMPONENT, SECURITY);

  private DatabaseInstanceNames() {
    // no construction
  }
}
