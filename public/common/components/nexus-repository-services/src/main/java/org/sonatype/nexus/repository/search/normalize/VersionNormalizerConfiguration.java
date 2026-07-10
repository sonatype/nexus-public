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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers shared version normalizers under multiple format qualifiers.
 *
 * Formats that share identical version sorting logic (e.g., all SemVer 2.0 formats)
 * reuse the same normalizer instance rather than creating empty subclasses.
 *
 * Note: formats with format-specific normalization rules (e.g. go, rubygems) have their own
 * dedicated @Component classes and are not aliased here.
 */
@Configuration
public class VersionNormalizerConfiguration
{
  // --- SemVer 2.0 formats (same logic as npm) ---

  @Bean
  @Qualifier("helm")
  VersionNormalizer helmNormalizer(@Qualifier("npm") final SemVerVersionNormalizer semver) {
    return semver;
  }

  @Bean
  @Qualifier("pub")
  VersionNormalizer pubNormalizer(@Qualifier("npm") final SemVerVersionNormalizer semver) {
    return semver;
  }

  @Bean
  @Qualifier("terraform")
  VersionNormalizer terraformNormalizer(@Qualifier("npm") final SemVerVersionNormalizer semver) {
    return semver;
  }

  @Bean
  @Qualifier("swift")
  VersionNormalizer swiftNormalizer(@Qualifier("npm") final SemVerVersionNormalizer semver) {
    return semver;
  }

}
