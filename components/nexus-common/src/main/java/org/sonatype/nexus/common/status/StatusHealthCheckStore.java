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
package org.sonatype.nexus.common.status;

/**
 * Store used for persisting a timestamp representing the most recent health check request received by the instance
 *
 * @since 3.15
 */
public interface StatusHealthCheckStore
{
  /**
   * Update the latest timestamp which represents the last health check request. In HA-C this time should be
   * persisted per node.
   */
  void markHealthCheckTime() throws StatusHealthCheckException;

  /**
   * Checks that db is readable.
   *
   * @throws StatusHealthCheckException if the db read check fails
   *
   * @since 3.16
   */
  void checkReadHealth() throws StatusHealthCheckException;
}
