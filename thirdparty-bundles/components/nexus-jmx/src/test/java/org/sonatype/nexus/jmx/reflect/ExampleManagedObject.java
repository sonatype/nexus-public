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
package org.sonatype.nexus.jmx.reflect;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.jmx.ObjectNameEntry;

/**
 * ???
 */
@Named
@Singleton
@ManagedObject(
    domain = "org.sonatype.nexus.jmx",
    entries = {
        @ObjectNameEntry(name="foo", value="bar")
    },
    description = "Example managed object"
)
public class ExampleManagedObject
{
  private String name;

  private String password;

  // R/W attribute

  @ManagedAttribute
  public String getName() {
    return name;
  }

  @ManagedAttribute
  public void setName(final String name) {
    this.name = name;
  }

  // W-only attribute

  public String getPassword() {
    return password;
  }

  @ManagedAttribute(
      description = "Set password"
  )
  public void setPassword(final String password) {
    this.password = password;
  }

  // Operation

  @ManagedOperation(
      description = "Reset name"
  )
  public void resetName() {
    this.name = null;
  }
}
