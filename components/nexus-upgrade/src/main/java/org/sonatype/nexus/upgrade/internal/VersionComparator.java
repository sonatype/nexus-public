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
package org.sonatype.nexus.upgrade.internal;

import java.util.Comparator;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

/**
 * Implementation of {@link Comparator} for comparing {@link Version} Strings, specifically using a {@link
 * GenericVersionScheme}
 *
 * @since 3.1
 */
public class VersionComparator
    implements Comparator<String>
{
  private final GenericVersionScheme versionScheme = new GenericVersionScheme();

  public static final Comparator<String> INSTANCE = new VersionComparator();

  private Version parseVersion(final String version) {
    try {
      return versionScheme.parseVersion(version);
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public int compare(final String o1, final String o2) {
    return parseVersion(o1).compareTo(parseVersion(o2));
  }
}
