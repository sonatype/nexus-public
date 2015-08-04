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
package org.sonatype.nexus.scheduling;

import org.sonatype.scheduling.SchedulerTask;

/**
 * The base interface for all Tasks used in Nexus.
 *
 * @author cstamas
 */
public interface NexusTask<T>
    extends SchedulerTask<T>
{

  /**
   * Prefix for rpivate properties keys.
   */
  static final String PRIVATE_PROP_PREFIX = ".";

  /**
   * Key of id property (private).
   */
  static final String ID_KEY = PRIVATE_PROP_PREFIX + "id";

  /**
   * Key of name property (private).
   */
  static final String NAME_KEY = PRIVATE_PROP_PREFIX + "name";

  /**
   * Key of alert email property (private).
   */
  static final String ALERT_EMAIL_KEY = PRIVATE_PROP_PREFIX + "alertEmail";

  /**
   * Returns a unique ID of the task.
   *
   * @return task id (or null if not available)
   */
  String getId();

  /**
   * Returns a name of the task.
   *
   * @return task name (or null if not available)
   */
  String getName();

  boolean isExposed();

  /**
   * Should an alert email be sent?
   *
   * @return true if alert email is set (not null and not empty), false otherwise
   */
  boolean shouldSendAlertEmail();

  /**
   * Returns the email address to which an email should be sent in case of task failure.<br/>
   * If the alert email is not set (null or empty) no email should be sent.
   *
   * @return alert email
   */
  String getAlertEmail();
}
