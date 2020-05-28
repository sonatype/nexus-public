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

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Metadata model will be serialized into Json representation for npm audit report
 *
 * @since 3.24
 */
public class Metadata
{
  private final VulnerabilityReport vulnerabilities;

  private final int dependencies;

  @SerializedName("devDependencies")
  private final int devDependencies;

  @SerializedName("optionalDependencies")
  private final int optionalDependencies;

  @SerializedName("totalDependencies")
  private final int totalDependencies;

  public Metadata(
      final VulnerabilityReport vulnerabilities,
      final int dependencies,
      final int devDependencies,
      final int optionalDependencies,
      final int totalDependencies)
  {
    this.vulnerabilities = vulnerabilities;
    this.dependencies = dependencies;
    this.devDependencies = devDependencies;
    this.optionalDependencies = optionalDependencies;
    this.totalDependencies = totalDependencies;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Metadata metadata = (Metadata) o;
    return dependencies == metadata.dependencies &&
        devDependencies == metadata.devDependencies &&
        optionalDependencies == metadata.optionalDependencies &&
        totalDependencies == metadata.totalDependencies &&
        Objects.equals(vulnerabilities, metadata.vulnerabilities);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vulnerabilities, dependencies, devDependencies, optionalDependencies, totalDependencies);
  }

  @Override
  public String toString() {
    return String.format(
        "dep [%d] dev dep [%d] optional dep [%d] total dep [%d]; vulnerabilities: %s",
        dependencies, devDependencies, optionalDependencies, totalDependencies, vulnerabilities);
  }
}
