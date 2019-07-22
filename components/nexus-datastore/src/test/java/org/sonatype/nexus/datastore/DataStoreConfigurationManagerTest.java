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
package org.sonatype.nexus.datastore;

import java.io.UncheckedIOException;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DataStoreConfigurationManager} tests.
 */
public class DataStoreConfigurationManagerTest
    extends TestSupport
{
  @Mock
  private Map<String, DataStoreConfigurationSource> configurationSources;

  private DataStoreConfigurationManager underTest;

  @Before
  public void setUp() {
    underTest = new DataStoreConfigurationManager(configurationSources);
  }

  private DataStoreConfiguration newDataStoreConfiguration(final String name, final String source) {
    DataStoreConfiguration config = new DataStoreConfiguration();
    config.setName(name);
    config.setType("testType");
    config.setSource(source);
    config.setAttributes(ImmutableMap.of());
    return config;
  }

  @Test
  public void namedConfigIsOnlyLoadedOnce() {
    DataStoreConfigurationSource sourceA = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceB = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceC = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceD = mock(DataStoreConfigurationSource.class);

    DataStoreConfiguration configA = newDataStoreConfiguration("config", "sourceA");
    DataStoreConfiguration configB = newDataStoreConfiguration("CONFIG", "sourceB");
    DataStoreConfiguration configC = newDataStoreConfiguration("Config", "sourceC");

    DataStoreConfiguration maven = newDataStoreConfiguration("maven", "sourceA");
    DataStoreConfiguration docker = newDataStoreConfiguration("docker", "sourceB");
    DataStoreConfiguration nuget = newDataStoreConfiguration("nuget", "sourceC");

    when(sourceA.isEnabled()).thenReturn(true);
    when(sourceA.browseStoreNames()).thenReturn(ImmutableList.of("config", "maven"));
    when(sourceA.load("config")).thenReturn(configA);
    when(sourceA.load("maven")).thenReturn(maven);

    when(sourceB.isEnabled()).thenReturn(true);
    when(sourceB.browseStoreNames()).thenReturn(ImmutableList.of("docker", "CONFIG"));
    when(sourceB.load("CONFIG")).thenReturn(configB);
    when(sourceB.load("docker")).thenReturn(docker);

    when(sourceC.isEnabled()).thenReturn(false);
    when(sourceC.browseStoreNames()).thenReturn(ImmutableList.of("nuget", "Config"));
    when(sourceC.load("Config")).thenReturn(configC);
    when(sourceC.load("nuget")).thenReturn(nuget);

    when(sourceD.isEnabled()).thenReturn(true);
    when(sourceD.browseStoreNames()).thenReturn(ImmutableList.of("config", "pypi"));
    when(sourceD.load("config")).thenThrow(UncheckedIOException.class);
    when(sourceD.load("pypi")).thenThrow(UncheckedIOException.class);

    InOrder expected = inOrder(sourceA, sourceB, sourceC, sourceD);

    // config from B is loaded first as C is disabled, other sources are not checked for config
    when(configurationSources.values()).thenReturn(ImmutableList.of(sourceC, sourceB, sourceD, sourceA));
    assertThat(underTest.load(), contains(docker, configB, maven));
    expected.verify(sourceB).load(matches("docker"));
    expected.verify(sourceB).load(matches("(?i)config"));
    expected.verify(sourceD).load(matches("pypi"));
    expected.verify(sourceA).load(matches("maven"));

    // config from D is loaded first but fails to load, other sources are not checked for config
    //
    // The reasoning behind this is that the user wanted to load the config from source D because
    // they put it first, but that source is broken - rather than silently falling back to another
    // source, which might have a valid config but not the one the user expects, we deliberately
    // avoid loading that named config again. The missing config will be detected when something
    // attempts to use it, alerting the user so they can fix the source.
    //
    when(configurationSources.values()).thenReturn(ImmutableList.of(sourceD, sourceC, sourceB, sourceA));
    assertThat(underTest.load(), contains(docker, maven));
    expected.verify(sourceD).load(matches("(?i)config"));
    expected.verify(sourceD).load(matches("pypi"));
    expected.verify(sourceB).load(matches("docker"));
    expected.verify(sourceA).load(matches("maven"));

    // config from A is loaded first, other sources are not checked for config
    when(configurationSources.values()).thenReturn(ImmutableList.of(sourceA, sourceB, sourceC, sourceD));
    assertThat(underTest.load(), contains(configA, maven, docker));
    expected.verify(sourceA).load(matches("(?i)config"));
    expected.verify(sourceA).load(matches("maven"));
    expected.verify(sourceB).load(matches("docker"));
    expected.verify(sourceD).load(matches("pypi"));

    expected.verifyNoMoreInteractions();
  }

  @Test
  public void configSavedToOriginalSource() {
    DataStoreConfigurationSource sourceA = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceB = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceC = mock(DataStoreConfigurationSource.class);

    InOrder expected = inOrder(sourceA, sourceB, sourceC);

    DataStoreConfiguration testConfig = newDataStoreConfiguration("sample", "B");

    // missing source
    try {
      underTest.save(testConfig);
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("refers to a missing source"));
    }

    // source exists, but is not modifiable
    when(configurationSources.get("B")).thenReturn(sourceB);
    try {
      underTest.save(testConfig);
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("is from a read-only source"));
    }

    // source exists, and is modifiable
    when(sourceB.isModifiable()).thenReturn(true);
    underTest.save(testConfig);
    expected.verify(sourceB).save(testConfig);

    expected.verifyNoMoreInteractions();
  }

  @Test
  public void configDeletedFromOriginalSource() {
    DataStoreConfigurationSource sourceA = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceB = mock(DataStoreConfigurationSource.class);
    DataStoreConfigurationSource sourceC = mock(DataStoreConfigurationSource.class);

    InOrder expected = inOrder(sourceA, sourceB, sourceC);

    DataStoreConfiguration testConfig = newDataStoreConfiguration("sample", "B");

    // missing source
    try {
      underTest.delete(testConfig);
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("refers to a missing source"));
    }

    // source exists, but is not modifiable
    when(configurationSources.get("B")).thenReturn(sourceB);
    try {
      underTest.delete(testConfig);
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("is from a read-only source"));
    }

    // source exists, and is modifiable
    when(sourceB.isModifiable()).thenReturn(true);
    underTest.delete(testConfig);
    expected.verify(sourceB).delete(testConfig);

    expected.verifyNoMoreInteractions();
  }
}
