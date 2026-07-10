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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link VersionNormalizerConfiguration} aliases the SemVer normalizer
 * for helm/pub/terraform/swift formats — i.e., it returns the SAME instance for all
 * SemVer-compatible formats rather than creating empty subclasses.
 */
public class VersionNormalizerConfigurationTest
{
  private VersionNormalizerConfiguration underTest;

  private SemVerVersionNormalizer semver;

  @Before
  public void setup() {
    underTest = new VersionNormalizerConfiguration();
    semver = new SemVerVersionNormalizer();
  }

  @Test
  public void testHelmAliasesSemVer() {
    VersionNormalizer helm = underTest.helmNormalizer(semver);
    assertThat(helm).isSameAs(semver);
  }

  @Test
  public void testPubAliasesSemVer() {
    VersionNormalizer pub = underTest.pubNormalizer(semver);
    assertThat(pub).isSameAs(semver);
  }

  @Test
  public void testTerraformAliasesSemVer() {
    VersionNormalizer terraform = underTest.terraformNormalizer(semver);
    assertThat(terraform).isSameAs(semver);
  }

  @Test
  public void testSwiftAliasesSemVer() {
    VersionNormalizer swift = underTest.swiftNormalizer(semver);
    assertThat(swift).isSameAs(semver);
  }

  @Test
  public void testAllFourAliasesShareSameInstance() {
    VersionNormalizer helm = underTest.helmNormalizer(semver);
    VersionNormalizer pub = underTest.pubNormalizer(semver);
    VersionNormalizer terraform = underTest.terraformNormalizer(semver);
    VersionNormalizer swift = underTest.swiftNormalizer(semver);

    assertThat(helm).isSameAs(pub).isSameAs(terraform).isSameAs(swift);
  }
}
