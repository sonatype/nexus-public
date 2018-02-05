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
package org.sonatype.nexus.quartz.internal.orient;

import org.quartz.Calendar;

/**
 * {@link Calendar} entity.
 *
 * @since 3.0
 */
public class CalendarEntity
  extends MarshalledEntity<Calendar>
{
  private String name;

  public CalendarEntity() {
    // empty
  }

  public CalendarEntity(final String name, final Calendar calendar) {
    setName(name);
    setValue(calendar);
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "name='" + name + '\'' +
        ", value=" + getValue() +
        '}';
  }
}
