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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class ProNexusEditionTest
    extends TestSupport
{
  @Mock
  private Path workDirPath;

  @Mock
  private File proEditionMarker;

  @Spy
  private NexusEdition underTest = new ProNexusEdition()
  {
    @Override
    public File getEditionMarker(final Path workDirPath) {
      return null;
    }
  };

  private final Boolean hasLoadAsOss;

  private final Boolean proMarkerExists;

  private final Boolean hasLoadAsStarter;

  private final Boolean nullPrefLic;

  private final Boolean is_pro;

  private final Boolean hasProFeature;

  private final Properties properties = new Properties();

  public ProNexusEditionTest(
      final Boolean hasLoadAsOss,
      final Boolean proMarkerExists,
      final Boolean nullPrefLic,
      final Boolean hasLoadAsStarter,
      final Boolean hasProFeature,
      final Boolean is_pro)
  {
    this.hasLoadAsOss = hasLoadAsOss;
    this.proMarkerExists = proMarkerExists;
    this.hasLoadAsStarter = hasLoadAsStarter;
    this.nullPrefLic = nullPrefLic;
    this.is_pro = is_pro;
    this.hasProFeature = hasProFeature;
  }

  @Parameters(name = "{index}: hasLoadAsOss: {0}, proMarker: {1}, nullPrefLic: {2}, hasLoadAsStarter: {3}, " +
      "hasProFeature: {4},is_pro: {5}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // if nexus.loadAsOSS has a value and, it's true then is_pro = false
        {true, true, false, false, false, false},
        {true, false, false, false, false, false},
        {true, true, true, false, false, false},
        {true, true, false, true, false, false},
        {true, true, false, false, true, false},
        // if hasLoadAsStarter is true then is_pro = false
        {false, true, false, true, false, false},
        {false, false, false, true, false, false},
        {false, true, true, true, false, false},
        {false, true, false, true, true, false},
        //if proMarker exists then is_pro = true
        {false, true, false, false, false, true},
        {false, true, true, false, false, true},
        {false, true, false, false, true, true},
        //nullPrefLic is not null and hasProFeature is true then is_pro = true
        {false, false, false, false, true, true},
        //nullPrefLic is null then is_pro ==false
        {false, false, true, false, true, false},
        //hasFeature is false then is_pro ==false
        {false, false, true, false, false, false}
    });
  }

  @Test
  public void testProShouldBoot() {
    properties.put("nexus-features", "fake-feature01,fake-feature02");

    when(underTest.hasNexusLoadAs(NexusEdition.NEXUS_LOAD_AS_OSS_PROP_NAME)).thenReturn(hasLoadAsOss);
    when(proEditionMarker.exists()).thenReturn(proMarkerExists);
    when(underTest.hasNexusLoadAs(NexusEdition.NEXUS_LOAD_AS_PRO_STARTER_PROP_NAME)).thenReturn(hasLoadAsStarter);
    when(underTest.isNullJavaPrefLicensePath("/com/sonatype/nexus/professional")).thenReturn(nullPrefLic);
    when(underTest.hasFeature(properties, NexusEditionFeature.PRO_FEATURE.featureString)).thenReturn(hasProFeature);

    when(underTest.getEditionMarker(any())).thenReturn(proEditionMarker);

    assertThat(underTest.applies(properties, workDirPath), is(is_pro));
  }
}
