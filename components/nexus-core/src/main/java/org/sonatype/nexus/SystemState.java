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
package org.sonatype.nexus;

/**
 * The enum of possible states in which Nexus Application may reside.
 *
 * @author cstamas
 */
public enum SystemState
{
  /**
   * Nexus is in process of starting. Should not be bothered until it is RUNNING.
   */
  STARTING,

  /**
   * Nexus is running and is healthy. It is fully functional.
   */
  STARTED,

  /**
   * Nexus tried to start up, but is failed due to broken user configuration. It is nonfunctional.
   */
  BROKEN_CONFIGURATION,

  /**
   * Nexus tried to start up, but is failed due to some unexpected IO error. It is nonfunctional.
   */
  BROKEN_IO,

  /**
   * Nexus is being shutdown.
   */
  STOPPING,

  /**
   * Nexus is shut down.
   */
  STOPPED;
}
