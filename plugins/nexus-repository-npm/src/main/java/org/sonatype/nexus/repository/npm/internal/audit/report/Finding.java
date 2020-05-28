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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.List;
import java.util.Objects;

/**
 * Model will be serialized into Json representation for npm audit report.
 *
 * @since 3.24
 */
public class Finding
{
  private final String version;

  private final List<String> paths;

  public Finding(final String version, final List<String> paths) {
    this.version = version;
    this.paths = paths;
  }

  public String getVersion() {
    return version;
  }

  public List<String> getPaths() {
    return paths;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Finding finding = (Finding) o;
    return Objects.equals(version, finding.version) &&
        Objects.equals(paths, finding.paths);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, paths);
  }
}
