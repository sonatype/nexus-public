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
package org.sonatype.nexus.rest.indexng;

import org.sonatype.aether.version.Version;

/**
 * Special version wrapper that uses original (string) version representation to determine version equality but aether
 * Version to determine if one version is greater than another. In other words, it provides proper version order
 * (according to Maven versioning rules), but treats "2" and "2.0" as two distinct versions.
 */
class StringVersion
    implements Comparable<StringVersion>
{
  private final String string;

  private final Version version;

  public StringVersion(String string, Version version) {
    this.string = string;
    this.version = version;
  }

  @Override
  public int hashCode() {
    return string.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof StringVersion)) {
      return false;
    }

    return string.equals(((StringVersion) obj).string);
  }

  @Override
  public int compareTo(StringVersion other) {
    int c = version.compareTo(other.version);
    if (c != 0) {
      return c;
    }
    return string.compareTo(other.string);
  }

}
