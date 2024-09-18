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
package org.sonatype.nexus.capability;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Type of a capability.
 *
 * @since capabilities 2.0
 */
public class CapabilityType
{

  @NotNull
  @CapabilityTypeExists
  private final String typeId;

  public CapabilityType(final String typeId) {
    this.typeId = checkNotNull(typeId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CapabilityType)) {
      return false;
    }

    final CapabilityType that = (CapabilityType) o;

    if (typeId != null ? !typeId.equals(that.typeId) : that.typeId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return typeId != null ? typeId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return typeId;
  }

  public static CapabilityType capabilityType(final String typeId) {
    return new CapabilityType(typeId);
  }

}
