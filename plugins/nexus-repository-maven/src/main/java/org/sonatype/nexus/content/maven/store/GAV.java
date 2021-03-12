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

package org.sonatype.nexus.content.maven.store;

import com.google.common.base.Objects;

/**
 *
 * Struct to track GAV we need to request metadata rebuild due to deletion.
 *
 * @since 3.30
 */
public class GAV
{
  public final String group;

  public final String name;

  public final String baseVersion;

  final int count;

  public GAV(final String group, final String name, final String baseVersion, final int count) {
    this.group = group;
    this.name = name;
    this.baseVersion = baseVersion;
    this.count = count;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GAV gav = (GAV) o;
    return count == gav.count && Objects.equal(group, gav.group) && Objects.equal(name, gav.name) &&
        Objects.equal(baseVersion, gav.baseVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(group, name, baseVersion, count);
  }

  @Override
  public String toString() {
    return "GAV{" + "group='" + group + '\'' + ", name='" + name + '\'' + ", baseVersion='" + baseVersion + '\'' +
        ", count=" + count + '}';
  }
}
