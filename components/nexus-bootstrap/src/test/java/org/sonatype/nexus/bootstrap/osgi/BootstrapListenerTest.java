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
package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class BootstrapListenerTest
    extends TestSupport
{
  @Parameters(name = "{index}: hasLoadAsOss: {0}, loadAsOss: {1}, edition_pro: {2}, "
      + "clustered: {3}, nullFileLic: {4}, nullPrefLic: {5}")
  public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
        // if nexus.loadAsOSS has a value and it's true then is_oss == true
        { true,  true,  false, false, true,  true,  true},
        { true,  true,  true,  false, true,  true,  true},
        { true,  true,  false, true,  true,  true,  true},
        { true,  true,  true,  true,  true,  true,  true},
        { true,  true,  false, false, false, true,  true},
        { true,  true,  true,  false, false, true,  true},
        { true,  true,  false, true,  false, true,  true},
        { true,  true,  true,  true,  false, true,  true},
        { true,  true,  false, false, true,  false, true},
        { true,  true,  true,  false, true,  false, true},
        { true,  true,  false, true,  true,  false, true},
        { true,  true,  true,  true,  true,  false, true},
        { true,  true,  false, false, false, false, true},
        { true,  true,  true,  false, false, false, true},
        { true,  true,  false, true,  false, false, true},
        { true,  true,  true,  true,  false, false, true},
        // if nexus.loadAsOSS has a value and it's false then is_oss == false
        { true,  false, true,  false, true,  true,  false},
        { true,  false, false, false, true,  true,  false},
        { true,  false, true,  true,  true,  true,  false},
        { true,  false, false, true,  true,  true,  false},
        { true,  false, true,  false, false, true,  false},
        { true,  false, false, false, false, true,  false},
        { true,  false, true,  true,  false, true,  false},
        { true,  false, false, true,  false, true,  false},
        { true,  false, true,  false, true,  false, false},
        { true,  false, false, false, true,  false, false},
        { true,  false, true,  true,  true,  false, false},
        { true,  false, false, true,  true,  false, false},
        { true,  false, true,  false, false, false, false},
        { true,  false, false, false, false, false, false},
        { true,  false, true,  true,  false, false, false},
        { true,  false, false, true,  false, false, false},
        // if nexus.loadAsOss doesn't have a value
        { false, false, false, false, true,  true,  true},
        // edition_pro is present then is_oss = false
        { false, false, true,  false, true,  true,  false},
        { false, false, true,  true,  true,  true,  false},
        { false, false, true,  false, false, true,  false},
        { false, false, true,  true,  false, true,  false},
        { false, false, true,  false, true,  false, false},
        { false, false, true,  true,  true,  false, false},
        { false, false, true,  false, false, false, false},
        { false, false, true,  true,  false, false, false},
        // if clustered then is_oss = false
        { false, false, false, true,  true,  true,  false},
        { false, false, false, true,  false, true,  false},
        { false, false, false, true,  true,  false, false},
        { false, false, false, true,  false, false, false},
        // if nexus.licenseFile is not null then is_oss = false
        { false, false, false, false, false, true,  false},
        { false, false, false, false, false, false, false},
        // if there is a license stored in javaprefs then is_oss = false
        { false, false, false, false, true,  false, false}
     });
  }

  @Spy
  private BootstrapListener underTest = new BootstrapListener();

  @Mock
  private Path workDirPath;

  @Mock
  private File proEditionMarker;

  private Boolean hasLoadAsOss;

  private Boolean loadAsOss;

  private Boolean edition_pro;

  private Boolean clustered;

  private Boolean nullFileLic;

  private Boolean nullPrefLic;

  private Boolean is_oss;

  public BootstrapListenerTest(
      final Boolean hasLoadAsOss,
      final Boolean loadAsOss,
      final Boolean edition_pro,
      final Boolean clustered,
      final Boolean nullFileLic,
      final Boolean nullPrefLic,
      final Boolean is_oss)
  {
    this.hasLoadAsOss = hasLoadAsOss;
    this.loadAsOss = loadAsOss;
    this.edition_pro = edition_pro;
    this.clustered = clustered;
    this.nullFileLic = nullFileLic;
    this.nullPrefLic = nullPrefLic;
    this.is_oss = is_oss;
  }

  @Test
  public void test() {
    Path editionPro = mock(Path.class);
    when(editionPro.toFile()).thenReturn(proEditionMarker);
    when(workDirPath.resolve("edition_pro")).thenReturn(editionPro);
    when(underTest.hasNexusLoadAsOSS()).thenReturn(hasLoadAsOss);
    when(underTest.isNexusLoadAsOSS()).thenReturn(loadAsOss);
    when(underTest.isNexusClustered()).thenReturn(clustered);
    when(underTest.isNullNexusLicenseFile()).thenReturn(nullFileLic);
    when(underTest.isNullJavaPrefLicense()).thenReturn(nullPrefLic);
    when(proEditionMarker.exists()).thenReturn(edition_pro);

    assertThat(underTest.shouldSwitchToOss(workDirPath), is(is_oss));
  }
}
