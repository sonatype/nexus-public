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
import java.util.Properties;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ProStarterNexusEditionTest
    extends TestSupport
{
  @Mock
  private Path workDirPath;

  @Mock
  private File proStarterEditionMarker;

  @Spy
  private NexusEdition underTest = new ProStarterNexusEdition()
  {
    @Override
    public File getEditionMarker(final Path workDirPath) {
      return null;
    }
  };

  private final Boolean hasLoadAsOss;

  private final Boolean proStarterMarkerExists;

  private final Boolean hasLoadAsStarter;

  private final Boolean nullPrefLic;

  private final Boolean is_pro_starter;

  private final Boolean hasProStarterFeature;

  private final Properties properties = new Properties();

  public ProStarterNexusEditionTest(
      final Boolean hasLoadAsOss,
      final Boolean proStarterMarkerExists,
      final Boolean nullPrefLic,
      final Boolean hasLoadAsStarter,
      final Boolean hasProStarterFeature,
      final Boolean is_pro_starter
  )
  {
    this.hasLoadAsOss = hasLoadAsOss;
    this.proStarterMarkerExists = proStarterMarkerExists;
    this.hasLoadAsStarter = hasLoadAsStarter;
    this.nullPrefLic = nullPrefLic;
    this.is_pro_starter = is_pro_starter;
    this.hasProStarterFeature = hasProStarterFeature;
  }

  @Parameters(name = "{index}: hasLoadAsOss: {0}, proStarterMarker: {1}, nullPrefLic: {2}, hasLoadAsStarter: {3}, " +
      "hasProStarterFeature: {4},is_pro_starter: {5}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // if nexus.loadAsOSS has a value, and it's true then is_pro_starter = false
        {true, true, false, false, false, false},
        {true, false, false, false, false, false},
        {true, true, true, false, false, false},
        {true, true, false, true, false, false},
        {true, true, false, false, true, false},
        // if hasLoadAsStarter is true then is_pro_starter = true
        {false, true, false, true, false, true},
        {false, false, false, true, false, true},
        {false, true, true, true, false, true},
        {false, true, false, true, true, true},
        //if proStarterMarker exists then is_pro_starter = true
        {false, true, false, false, false, true},
        {false, true, true, false, false, true},
        {false, true, false, false, true, true},
        //nullPrefLic is not null and hasProStarterFeature is true then is_pro_starter = true
        {false, false, false, false, true, true},
        //nullPrefLic is null then is_pro_starter = false
        {false, false, true, false, true, false},
        //hasFeature is false then is_pro_starter = false
        {false, false, true, false, false, false}
    });
  }

  @Test
  public void testProStarterShouldBoot() {
    properties.put("nexus-features", "fake-feature01,fake-feature02");

    when(underTest.hasNexusLoadAs(NexusEdition.NEXUS_LOAD_AS_OSS_PROP_NAME)).thenReturn(hasLoadAsOss);
    when(proStarterEditionMarker.exists()).thenReturn(proStarterMarkerExists);
    when(underTest.hasNexusLoadAs(NexusEdition.NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME)).thenReturn(hasLoadAsStarter);
    when(underTest.isNullJavaPrefLicensePath("/com/sonatype/nexus/pro-starter")).thenReturn(nullPrefLic);
    when(underTest.hasFeature(properties, NexusEditionFeature.PRO_STARTER_FEATURE.featureString)).thenReturn(
        hasProStarterFeature);

    when(underTest.getEditionMarker(any())).thenReturn(proStarterEditionMarker);

    assertThat(underTest.applies(properties, workDirPath), is(is_pro_starter));
  }
}
