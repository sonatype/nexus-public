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
package org.sonatype.nexus.repository.npm.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractAlwaysPackageVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractNewestVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractPackageRootVersionUnlessEmpty;

public class NpmVersionComparatorTest
    extends TestSupport
{
  @Test
  public void shouldReturnPackage() throws Exception {
    assertThat(extractAlwaysPackageVersion.apply("1.0.0", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageRootWhenNotEmpty() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply("1.0.0", "2.0.0"), is("1.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenPackageRootIsEmpty() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply("", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenPackageRootIsNull() throws Exception {
    assertThat(extractPackageRootVersionUnlessEmpty.apply(null, "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("1.0.0", "2.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnPackageRootWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("2.0.0", "1.0.0"), is("2.0.0"));
  }

  @Test
  public void shouldReturnReleasePackageWhenLatest() throws Exception {
    assertThat(extractNewestVersion.apply("1.0.0", "1.0.0-SNAPSHOT"), is("1.0.0"));
  }
}
