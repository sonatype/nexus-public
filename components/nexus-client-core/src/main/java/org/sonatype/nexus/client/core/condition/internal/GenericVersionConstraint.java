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
package org.sonatype.nexus.client.core.condition.internal;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.util.Collection;
import java.util.HashSet;

/******
 * Copied from org.sonatype.aether:aether-util:1.13.1
 ******/

/**
 * A constraint on versions for a dependency.
 *
 * @author Benjamin Bentmann
 */
final class GenericVersionConstraint
    implements VersionConstraint
{

  private Collection<VersionRange> ranges = new HashSet<VersionRange>();

  private Version version;

  /**
   * Adds the specified version range to this constraint. All versions matched by the given range satisfy this
   * constraint.
   *
   * @param range The version range to add, may be {@code null}.
   * @return This constraint for chaining, never {@code null}.
   */
  public GenericVersionConstraint addRange(VersionRange range) {
    if (range != null) {
      ranges.add(range);
    }
    return this;
  }

  public Collection<VersionRange> getRanges() {
    return ranges;
  }

  /**
   * Sets the recommended version to satisfy this constraint.
   *
   * @param version The recommended version for this constraint, may be {@code null} if none.
   * @return This constraint for chaining, never {@code null}.
   */
  public GenericVersionConstraint setVersion(Version version) {
    this.version = version;
    return this;
  }

  public Version getVersion() {
    return version;
  }

  public boolean containsVersion(Version version) {
    if (ranges.isEmpty()) {
      return version.equals(this.version);
    }
    else {
      for (VersionRange range : ranges) {
        if (range.containsVersion(version)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder(128);

    for (VersionRange range : getRanges()) {
      if (buffer.length() > 0) {
        buffer.append(",");
      }
      buffer.append(range);
    }

    if (buffer.length() <= 0) {
      buffer.append(getVersion());
    }

    return buffer.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !getClass().equals(obj.getClass())) {
      return false;
    }

    GenericVersionConstraint that = (GenericVersionConstraint) obj;

    return ranges.equals(that.getRanges()) && eq(version, that.getVersion());
  }

  private static <T> boolean eq(T s1, T s2) {
    return s1 != null ? s1.equals(s2) : s2 == null;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + hash(getRanges());
    hash = hash * 31 + hash(getVersion());
    return hash;
  }

  private static int hash(Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }

}
