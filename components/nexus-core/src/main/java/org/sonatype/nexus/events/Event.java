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
package org.sonatype.nexus.events;

import java.util.Date;
import java.util.Map;

public interface Event<T>
{
  /**
   * Returns the timestamp of the creation of this event object. It's usage is left for consumer of this event (or
   * creator).
   */
  Date getEventDate();

  /**
   * Returns the modifiable event context. It may be used for some sort of data or object passing between event
   * consumer. This interface is not guaranteeing any processing order, so it is left to user of this api to sort
   * this
   * out.
   */
  Map<Object, Object> getEventContext();

  /**
   * Returns the event sender/initiator.
   */
  T getEventSender();
}
