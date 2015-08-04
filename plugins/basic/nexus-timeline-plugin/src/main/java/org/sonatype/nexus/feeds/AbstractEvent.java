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
package org.sonatype.nexus.feeds;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.RequestContext;

public class AbstractEvent
{
  /**
   * The date of event.
   */
  private final Date eventDate;

  /**
   * The action.
   */
  private final String action;

  /**
   * Human message/descritpion.
   */
  private final String message;

  /**
   * The context of event.
   */
  private final Map<String, Object> eventContext;

  public AbstractEvent(final Date eventDate, final String action, final String message) {
    this.eventDate = eventDate;

    this.action = action;

    this.message = message;

    this.eventContext = new HashMap<String, Object>();
  }

  public Date getEventDate() {
    return eventDate;
  }

  public String getAction() {
    return action;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, Object> getEventContext() {
    return eventContext;
  }

  public void addEventContext(Map<String, ?> ctx) {
    if (ctx instanceof RequestContext) {
      getEventContext().putAll(((RequestContext) ctx).flatten());
    }
    else {
      getEventContext().putAll(ctx);
    }
  }

  public String toString() {
    return getMessage();
  }
}
