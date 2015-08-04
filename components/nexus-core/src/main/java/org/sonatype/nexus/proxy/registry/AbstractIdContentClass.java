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
package org.sonatype.nexus.proxy.registry;

public abstract class AbstractIdContentClass
    implements ContentClass
{
  @Override
  public String getName() {
    return getId();
  }

  @Override
  public boolean isCompatible(ContentClass contentClass) {
    return getId().equals(contentClass.getId());
  }

  @Override
  public boolean isGroupable() {
    return true;
  }

  // ==

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }

    if (o instanceof ContentClass) {
      return getId().equals(((ContentClass) o).getId());
    }
    else {
      return false;
    }
  }
}
