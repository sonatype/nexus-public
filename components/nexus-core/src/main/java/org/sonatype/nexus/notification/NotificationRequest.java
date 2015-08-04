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
package org.sonatype.nexus.notification;

import java.util.HashSet;
import java.util.Set;

public class NotificationRequest
{
  public static final NotificationRequest EMPTY = new NotificationRequest(NotificationMessage.EMPTY_MESSAGE);

  private final Set<NotificationTarget> targets;

  private final NotificationMessage message;

  public NotificationRequest(NotificationMessage message) {
    this(message, new HashSet<NotificationTarget>());
  }

  public NotificationRequest(NotificationMessage message, Set<NotificationTarget> targets) {
    this.message = message;

    this.targets = targets;
  }

  public Set<NotificationTarget> getTargets() {
    return targets;
  }

  public NotificationMessage getMessage() {
    return message;
  }

  public boolean isEmpty() {
    return getTargets().isEmpty();
  }
}
