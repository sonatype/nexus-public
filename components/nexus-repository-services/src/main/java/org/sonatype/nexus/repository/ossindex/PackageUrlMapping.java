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
package org.sonatype.nexus.repository.ossindex;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.goodies.packageurl.PackageUrl;

/**
 * Represents a format-specific mapping of components to {@link PackageUrl}s.
 *
 * All formats should implement this to get vulnerability information
 * @see https://ossindex.sonatype.org/ecosystems
 *
 * @since 3.next
 */
public interface PackageUrlMapping
{
  /**
   * Returns {@link PackageUrl} for the given component coordinates.
   */
  Optional<PackageUrl> buildPackageUrl(@Nullable String namespace, String name, @Nullable String version);
}
