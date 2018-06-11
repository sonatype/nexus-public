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
package org.sonatype.nexus.repository.pypi.internal;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Search result for PyPI.
 *
 * @since 3.1
 */
final class PyPiSearchResult
{
  private final String name;

  private final String version;

  private final String summary;

  public PyPiSearchResult(@Nonnull final String name, @Nonnull final String version, @Nonnull final String summary) {
    this.name = checkNotNull(name);
    this.version = checkNotNull(version);
    this.summary = checkNotNull(summary);
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getSummary() {
    return summary;
  }
}
