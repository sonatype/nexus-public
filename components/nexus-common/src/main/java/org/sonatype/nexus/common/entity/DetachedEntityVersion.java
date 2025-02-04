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
package org.sonatype.nexus.common.entity;

import java.io.Serializable;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Detached {@link EntityVersion}.
 *
 * @since 3.0
 */
public class DetachedEntityVersion
    implements EntityVersion, Serializable
{
  private static final long serialVersionUID = 1L;

  private final String value;

  public DetachedEntityVersion(final String value) {
    this.value = checkNotNull(value);
  }

  @Override
  @Nonnull
  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    else if (!(o instanceof EntityVersion)) {
      return false;
    }

    EntityVersion that = (EntityVersion) o;
    return getValue().equals(that.getValue());
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "value='" + value + '\'' +
        '}';
  }
}
