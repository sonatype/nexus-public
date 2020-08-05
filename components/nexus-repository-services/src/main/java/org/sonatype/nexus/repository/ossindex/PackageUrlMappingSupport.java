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

import org.sonatype.goodies.packageurl.PackageUrl;
import org.sonatype.goodies.packageurl.PackageUrlBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.of;

/**
 * Support for {@link PackageUrlMapping}s.
 *
 * @since 3.26
 */
public abstract class PackageUrlMappingSupport
    implements PackageUrlMapping
{
  private final String format;

  protected PackageUrlMappingSupport(final String format) {
    this.format = checkNotNull(format);
  }

  @Override
  public Optional<PackageUrl> buildPackageUrl(final String namespace, final String name, final String version) {
    return of(new PackageUrlBuilder().type(format).namespace(namespace).name(name).version(version).build());
  }
}
