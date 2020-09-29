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
package org.sonatype.nexus.repository.r.internal.hosted;

import java.util.Locale;
import java.util.Objects;

import se.sawano.java.text.AlphanumericComparator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class representing a single valid R package version that can be compared to other versions. Version formats
 * explicitly supported/tested are those that include digits separated by dots or dashes, but this is not internally
 * enforced (and using other version schemes will result in undefined behavior).
 *
 * Refer to R's {@code package_version} and {@code utils::compareVersions} functions for implementation details.
 *
 * @since 3.28
 */
public class RPackageVersion
    implements Comparable<RPackageVersion>
{
  /**
   * An alphanumeric comparator that will produce the proper ordering for R version strings.
   */
  private static final AlphanumericComparator versionComparator = new AlphanumericComparator(Locale.US);

  /**
   * The version string as originally provided, maintaining its original format and separators.
   */
  private final String originalVersion;

  /**
   * The normalized version string with all dashes replaced with dots for quick alphanumeric comparison.
   */
  private final String normalizedVersion;

  /**
   * Constructor.
   *
   * @param version The version string.
   */
  public RPackageVersion(final String version) {
    this.originalVersion = checkNotNull(version).trim();
    this.normalizedVersion = originalVersion.replace("-", ".");
  }

  /**
   * Compares two {@code RPackageVersion}s with the assumption that both are valid version strings.
   *
   * @param other The {@code RPackageVersion} with which to compare.
   * @return Integer indicating if the version is less than, equal to, or greater than this version.
   */
  @Override
  public int compareTo(final RPackageVersion other) {
    return versionComparator.compare(normalizedVersion, other.normalizedVersion);
  }

  /**
   * Returns whether or not this {@code RPackageVersion} is equal to another object. For purposes of the comparison,
   * the two will be equal if and only if both are {@code RPackageVersion}s and their normalized version strings are
   * equal.
   *
   * @param o The other object.
   * @return {@code true} if equal, {@code false} otherwise
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RPackageVersion that = (RPackageVersion) o;
    return Objects.equals(normalizedVersion, that.normalizedVersion);
  }

  /**
   * Returns a hash code for this {@code RPackageVersion} derived from the hash code for the normalized version string.
   *
   * @return The hash code for this {@code RPackageVersion}.
   */
  @Override
  public int hashCode() {
    return Objects.hash(normalizedVersion);
  }

  /**
   * Returns a string representation containing both the original version string and its normalized representation
   * internal to the instance.
   *
   * @return The string representation.
   */
  @Override
  public String toString() {
    return "RPackageVersion{" +
        "originalVersion='" + originalVersion + '\'' +
        ", normalizedVersion='" + normalizedVersion + '\'' +
        '}';
  }
}
