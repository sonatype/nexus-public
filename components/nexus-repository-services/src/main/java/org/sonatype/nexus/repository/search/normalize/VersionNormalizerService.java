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
package org.sonatype.nexus.repository.search.normalize;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class VersionNormalizerService
{
  private final Map<String, VersionNormalizer> versionNormalizers;

  @Inject
  public VersionNormalizerService(final Map<String, VersionNormalizer> versionNormalizers) {
    this.versionNormalizers = checkNotNull(versionNormalizers);
  }

  /**
   * Normalizes supplied version using default approach or format specific
   * if format specific implementation of {@link VersionNormalizer} is present
   * @param version version to be normalized
   * @param format format that component belongs to
   * @return normalized version
   */
  public String getNormalizedVersionByFormat(final String version, final Format format) {
    VersionNormalizer normalizerForFormat = versionNormalizers.get(format.getValue());
    if (Objects.nonNull(normalizerForFormat)) {
      return normalizerForFormat.getNormalizedVersion(version);
    }
    else {
      return VersionNumberExpander.expand(version);
    }
  }
}
