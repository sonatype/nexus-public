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
package org.sonatype.nexus.internal.log.overrides.datastore;

import org.sonatype.nexus.common.event.EventWithSource;

/**
 * An event fired when the logger overrides has changed in order to propagate changes to all nodes.
 */
public class LoggerOverridesEvent
    extends EventWithSource
{
  private String name;

  private String level;

  private Action action;

  public LoggerOverridesEvent() {
    // deserialization
  }

  public LoggerOverridesEvent(final String name, final String level, final Action action) {
    this.name = name;
    this.level = level;
    this.action = action;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(final String level) {
    this.level = level;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(final Action action) {
    this.action = action;
  }

  public enum Action
  {
    CHANGE, RESET, RESET_ALL
  }
}
