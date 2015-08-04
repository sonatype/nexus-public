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

/******
 * Copied from org.sonatype.aether:aether-util:1.13.1
 ******/

/**
 * A version range inspired by mathematical range syntax. For example, "[1.0,2.0)", "[1.0,)" or "[1.0]".
 *
 * @author Benjamin Bentmann
 * @author Alin Dreghiciu
 */
final class GenericVersionRange
    implements VersionRange
{

  private final Version lowerBound;

  private final boolean lowerBoundInclusive;

  private final Version upperBound;

  private final boolean upperBoundInclusive;

  /**
   * Creates a version range from the specified range specification.
   *
   * @param range The range specification to parse, must not be {@code null}.
   * @throws InvalidVersionSpecificationException
   *          If the range could not be parsed.
   */
  public GenericVersionRange(String range)
      throws InvalidVersionSpecificationException
  {
    String process = range;

    if (range.startsWith("[")) {
      lowerBoundInclusive = true;
    }
    else if (range.startsWith("(")) {
      lowerBoundInclusive = false;
    }
    else {
      throw new InvalidVersionSpecificationException(range, "Invalid version range " + range
          + ", a range must start with either [ or (");
    }

    if (range.endsWith("]")) {
      upperBoundInclusive = true;
    }
    else if (range.endsWith(")")) {
      upperBoundInclusive = false;
    }
    else {
      throw new InvalidVersionSpecificationException(range, "Invalid version range " + range
          + ", a range must end with either [ or (");
    }

    process = process.substring(1, process.length() - 1);

    int index = process.indexOf(",");

    if (index < 0) {
      if (!lowerBoundInclusive || !upperBoundInclusive) {
        throw new InvalidVersionSpecificationException(range, "Invalid version range " + range
            + ", single version must be surrounded by []");
      }

      lowerBound = upperBound = new GenericVersion(process.trim());
    }
    else {
      String parsedLowerBound = process.substring(0, index).trim();
      String parsedUpperBound = process.substring(index + 1).trim();

      // more than two bounds, e.g. (1,2,3)
      if (parsedUpperBound.contains(",")) {
        throw new InvalidVersionSpecificationException(range, "Invalid version range " + range
            + ", bounds may not contain additional ','");
      }

      lowerBound = parsedLowerBound.length() > 0 ? new GenericVersion(parsedLowerBound) : null;
      upperBound = parsedUpperBound.length() > 0 ? new GenericVersion(parsedUpperBound) : null;

      if (upperBound != null && lowerBound != null) {
        if (upperBound.compareTo(lowerBound) < 0) {
          throw new InvalidVersionSpecificationException(range, "Invalid version range " + range
              + ", lower bound must not be greater than upper bound");
        }
      }
    }
  }

  public GenericVersionRange(Version lowerBound, boolean lowerBoundInclusive, Version upperBound,
                             boolean upperBoundInclusive)
  {
    this.lowerBound = lowerBound;
    this.lowerBoundInclusive = lowerBoundInclusive;
    this.upperBound = upperBound;
    this.upperBoundInclusive = upperBoundInclusive;
  }

  public Version getLowerBound() {
    return lowerBound;
  }

  public boolean isLowerBoundInclusive() {
    return lowerBoundInclusive;
  }

  public Version getUpperBound() {
    return upperBound;
  }

  public boolean isUpperBoundInclusive() {
    return upperBoundInclusive;
  }

  public boolean containsVersion(Version version) {
    if (lowerBound != null) {
      int comparison = lowerBound.compareTo(version);

      if (comparison == 0 && !lowerBoundInclusive) {
        return false;
      }
      if (comparison > 0) {
        return false;
      }
    }

    if (upperBound != null) {
      int comparison = upperBound.compareTo(version);

      if (comparison == 0 && !upperBoundInclusive) {
        return false;
      }
      if (comparison < 0) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    else if (obj == null || !getClass().equals(obj.getClass())) {
      return false;
    }

    GenericVersionRange that = (GenericVersionRange) obj;

    return upperBoundInclusive == that.upperBoundInclusive && lowerBoundInclusive == that.lowerBoundInclusive
        && eq(upperBound, that.upperBound) && eq(lowerBound, that.lowerBound);
  }

  private static <T> boolean eq(T s1, T s2) {
    return s1 != null ? s1.equals(s2) : s2 == null;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + hash(upperBound);
    hash = hash * 31 + (upperBoundInclusive ? 1 : 0);
    hash = hash * 31 + hash(lowerBound);
    hash = hash * 31 + (lowerBoundInclusive ? 1 : 0);
    return hash;
  }

  private static int hash(Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder(64);
    buffer.append(lowerBoundInclusive ? '[' : '(');
    if (lowerBound != null) {
      buffer.append(lowerBound);
    }
    buffer.append(',');
    if (upperBound != null) {
      buffer.append(upperBound);
    }
    buffer.append(upperBoundInclusive ? ']' : ')');
    return buffer.toString();
  }

}
