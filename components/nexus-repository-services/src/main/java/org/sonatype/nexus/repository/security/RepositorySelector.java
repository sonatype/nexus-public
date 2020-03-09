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
package org.sonatype.nexus.repository.security;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A selector util class that builds values that we need for storage and display of the privileges to humans based
 * on the provided repo name and format
 *
 * @since 3.1
 */
public class RepositorySelector
{
  @VisibleForTesting
  public static final String ALL = "*";

  private static final String ALL_OF_FORMAT_PREFIX = "*-";

  private final String name;

  private final String format;

  private RepositorySelector(final String name, final String format) {
    this.name = checkNotNull(name);
    this.format = checkNotNull(format);
  }

  public static RepositorySelector all() {
    return new RepositorySelector(ALL, ALL);
  }

  public static RepositorySelector allOfFormat(final String format) {
    return new RepositorySelector(ALL, format);
  }

  public static RepositorySelector fromSelector(final String selector) {
    checkNotNull(selector);

    if (ALL.equals(selector)) {
      return new RepositorySelector(ALL, ALL);
    }
    else if (selector.startsWith(ALL_OF_FORMAT_PREFIX)) {
      return new RepositorySelector(ALL, selector.substring(ALL_OF_FORMAT_PREFIX.length()));
    }
    return new RepositorySelector(selector, ALL);
  }

  public static RepositorySelector fromNameAndFormat(final String name, final String format) {
    return new RepositorySelector(name, format);
  }

  public String getName() {
    return name;
  }

  public String getFormat() {
    return format;
  }

  public String toSelector() {
    if (ALL.equals(name)) {
      if (ALL.equals(format)) {
        return ALL;
      }
      return ALL_OF_FORMAT_PREFIX + format;
    }
    return name;
  }

  public String humanizeSelector() {
    if (ALL.equals(name)) {
      if (ALL.equals(format)) {
        return "all";
      }
      return "all '" + format + "'-format";
    }
    return name;
  }

  public boolean isAllRepositories() {
    return ALL.equals(name);
  }

  public boolean isAllFormats() {
    return ALL.equals(format);
  }
}
