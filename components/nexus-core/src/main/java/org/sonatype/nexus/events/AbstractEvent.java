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
import java.util.HashMap;
import java.util.Map;

/**
 * The superclass for all events.
 *
 * @author cstamas
 * @since 2.7.0
 */
public abstract class AbstractEvent<T>
    implements Event<T>
{
  /**
   * The event date.
   */
  private final Date eventDate;

  /**
   * The event context
   */
  private final HashMap<Object, Object> eventContext;

  /**
   * The sender
   */
  private final T eventSender;

  /**
   * Instantiates a new abstract event.
   */
  public AbstractEvent(T component) {
    this.eventDate = new Date();
    this.eventContext = new HashMap<Object, Object>();
    this.eventSender = component;
  }

  /**
   * Gets the event date.
   *
   * @return the event date
   */
  public Date getEventDate() {
    return eventDate;
  }

  /**
   * Gets the modifiable event context.
   *
   * @return the event context
   */
  public Map<Object, Object> getEventContext() {
    return eventContext;
  }

  /**
   * Gets the sender
   *
   * @return the event sender
   */
  public T getEventSender() {
    return eventSender;
  }
}
