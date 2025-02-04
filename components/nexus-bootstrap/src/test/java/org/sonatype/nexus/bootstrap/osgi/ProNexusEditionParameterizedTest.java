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
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.NEXUS_LOAD_AS_OSS_PROP_NAME;
import static org.sonatype.nexus.bootstrap.osgi.NexusEdition.PRO_LICENSE_LOCATION;

@RunWith(Parameterized.class)
public class ProNexusEditionParameterizedTest
    extends TestSupport
{
  @Spy
  private ProNexusEdition underTest = new ProNexusEdition();

  @Mock
  private Path workDirPath;

  @Mock
  private Path proPath;

  @Mock
  private File proEditionMarker;

  private Boolean hasLoadAsOss;

  private Boolean loadAsOss;

  private Boolean proMarkerExists;

  private Boolean hasLoadAsStarter;

  private Boolean loadAsStarter;

  private Boolean proStarterMarkerExists;

  private Boolean nullFileLic;

  private Boolean nullPrefLic;

  private Boolean is_oss;

  private Boolean is_starter;

  public ProNexusEditionParameterizedTest(
      final Boolean hasLoadAsOss,
      final Boolean loadAsOss,
      final Boolean proMarkerExists,
      final Boolean nullFileLic,
      final Boolean nullPrefLic,
      final Boolean hasLoadAsStarter,
      final Boolean loadAsStarter,
      final Boolean proStarterMarkerExists,
      final Boolean is_oss,
      final Boolean is_starter)
  {
    this.hasLoadAsOss = hasLoadAsOss;
    this.loadAsOss = loadAsOss;
    this.proMarkerExists = proMarkerExists;
    this.nullFileLic = nullFileLic;
    this.nullPrefLic = nullPrefLic;
    this.hasLoadAsStarter = hasLoadAsStarter;
    this.loadAsStarter = loadAsStarter;
    this.proStarterMarkerExists = proStarterMarkerExists;
    this.is_oss = is_oss;
    this.is_starter = is_starter;
  }

  @Parameters(name = "{index}: hasLoadAsOss: {0}, loadAsOss: {1}, proMarker: {2}, "
      +
      "nullFileLic: {3}, nullPrefLic: {4}, hasLoadAsStarter: {5}, loadAsStarter: {6}, starterMarker: {7}, is_oss: {8}, is_starter: {9}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // if nexus.loadAsOSS has a value and it's true then is_oss == true
        {true, true, false, true, true, false, false, false, true, false},
        {true, true, true, true, true, false, false, false, true, false},
        {true, true, false, true, true, false, false, false, true, false},
        {true, true, true, true, true, false, false, false, true, false},
        {true, true, false, false, true, false, false, false, true, false},
        {true, true, true, false, true, false, false, false, true, false},
        {true, true, false, false, true, false, false, false, true, false},
        {true, true, true, false, true, false, false, false, true, false},
        {true, true, false, true, false, false, false, false, true, false},
        {true, true, true, true, false, false, false, false, true, false},
        {true, true, false, true, false, false, false, false, true, false},
        {true, true, true, true, false, false, false, false, true, false},
        {true, true, false, false, false, false, false, false, true, false},
        {true, true, true, false, false, false, false, false, true, false},
        {true, true, false, false, false, false, false, false, true, false},
        {true, true, true, false, false, false, false, false, true, false},
        // if nexus.loadAsOSS has a value and it's false then is_oss == false
        {true, false, true, true, true, false, false, false, false, false},
        {true, false, false, true, true, false, false, false, false, false},
        {true, false, true, true, true, false, false, false, false, false},
        {true, false, false, true, true, false, false, false, false, false},
        {true, false, true, false, true, false, false, false, false, false},
        {true, false, false, false, true, false, false, false, false, false},
        {true, false, true, false, true, false, false, false, false, false},
        {true, false, false, false, true, false, false, false, false, false},
        {true, false, true, true, false, false, false, false, false, false},
        {true, false, false, true, false, false, false, false, false, false},
        {true, false, true, true, false, false, false, false, false, false},
        {true, false, false, true, false, false, false, false, false, false},
        {true, false, true, false, false, false, false, false, false, false},
        {true, false, false, false, false, false, false, false, false, false},
        {true, false, true, false, false, false, false, false, false, false},
        {true, false, false, false, false, false, false, false, false, false},
        // if nexus.loadAsOss doesn't have a value
        {false, false, false, true, true, false, false, false, true, false},
        // proMarker is present then is_oss = false
        {false, false, true, true, true, false, false, false, false, false},
        {false, false, true, true, true, false, false, false, false, false},
        {false, false, true, false, true, false, false, false, false, false},
        {false, false, true, false, true, false, false, false, false, false},
        {false, false, true, true, false, false, false, false, false, false},
        {false, false, true, true, false, false, false, false, false, false},
        {false, false, true, false, false, false, false, false, false, false},
        {false, false, true, false, false, false, false, false, false, false},
        // if clustered then is_oss = false
        {false, false, false, false, true, false, false, false, false, false},
        {false, false, false, true, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false},
        // if nexus.licenseFile is not null then is_oss = false
        {false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false},
        // if there is a license stored in javaprefs then is_oss = false
        {false, false, false, true, false, false, false, false, false, false},
    });
  }

  @Test
  public void testProShouldSwitchToOss() {
    when(workDirPath.resolve("edition_pro")).thenReturn(proPath);
    when(proPath.toFile()).thenReturn(proEditionMarker);
    when(proEditionMarker.exists()).thenReturn(proMarkerExists);
    when(underTest.hasNexusLoadAs(NEXUS_LOAD_AS_OSS_PROP_NAME)).thenReturn(hasLoadAsOss);
    when(underTest.isNexusLoadAs(NEXUS_LOAD_AS_OSS_PROP_NAME)).thenReturn(loadAsOss);
    when(underTest.isNullNexusLicenseFile()).thenReturn(nullFileLic);
    when(underTest.isNullJavaPrefLicensePath(PRO_LICENSE_LOCATION)).thenReturn(nullPrefLic);

    assertThat(underTest.shouldSwitchToFree(workDirPath), is(is_oss));
  }
}
