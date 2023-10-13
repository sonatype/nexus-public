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
package org.sonatype.nexus.cleanup.internal.content.retain;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.cleanup.CleanupFeatureCheck;
import org.sonatype.nexus.repository.content.kv.global.GlobalKeyValueStore;
import org.sonatype.nexus.repository.content.kv.global.NexusKeyValue;
import org.sonatype.nexus.repository.content.kv.global.ValueType;
import org.sonatype.nexus.repository.content.tasks.normalize.FormatVersionNormalizedEvent;
import org.sonatype.nexus.repository.content.tasks.normalize.NormalizeComponentVersionTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.app.FeatureFlags.FORMAT_RETAIN_PATTERN;

public class CleanupRetainStateContributorTest
    extends TestSupport
{
  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private CleanupFeatureCheck featureCheck;

  private List<Format> formats;

  private CleanupRetainStateContributor underTest;

  @Before
  public void setUp() {
    Format format1 = mock(Format.class);
    Format format2 = mock(Format.class);
    Format format3 = mock(Format.class);

    when(format1.getValue()).thenReturn("maven2");
    when(format2.getValue()).thenReturn("npm");
    when(format3.getValue()).thenReturn("docker");

    formats = Arrays.asList(format1, format2, format3);

    when(globalKeyValueStore.getKey(eq(format1.getValue())))
        .thenReturn(Optional.of(new NexusKeyValue("", ValueType.BOOLEAN, false)));
    when(globalKeyValueStore.getKey(eq(format2.getValue())))
        .thenReturn(Optional.of(new NexusKeyValue("", ValueType.BOOLEAN, false)));
    when(globalKeyValueStore.getKey(eq(format3.getValue())))
        .thenReturn(Optional.of(new NexusKeyValue("", ValueType.BOOLEAN, false)));

    //return retain support enabled for maven2 & docker
    when(featureCheck.isRetainSupported(eq(format1.getValue()))).thenReturn(true);
    when(featureCheck.isRetainSupported(eq(format2.getValue()))).thenReturn(false);
    when(featureCheck.isRetainSupported(eq(format3.getValue()))).thenReturn(true);

    underTest = new CleanupRetainStateContributor(formats, globalKeyValueStore, featureCheck);
  }

  @Test
  public void testStateReturnsInitialContent() {
    Map<String, Object> currentState = underTest.getState();

    assertNotNull(currentState);
    assertEquals(6, currentState.size());
    assertFalse((boolean) currentState.get(String.format(NormalizeComponentVersionTask.KEY_FORMAT, "maven2")));
    assertFalse((boolean) currentState.get(String.format(NormalizeComponentVersionTask.KEY_FORMAT, "npm")));
    assertFalse((boolean) currentState.get(String.format(NormalizeComponentVersionTask.KEY_FORMAT, "docker")));
    assertTrue((boolean) currentState.get(FORMAT_RETAIN_PATTERN.replace("{format}", "maven2")));
    assertFalse((boolean) currentState.get(FORMAT_RETAIN_PATTERN.replace("{format}", "npm")));
    assertTrue((boolean) currentState.get(FORMAT_RETAIN_PATTERN.replace("{format}", "docker")));
  }

  @Test
  public void testStateGetsUpdatedIfEventIsSent() {
    FormatVersionNormalizedEvent event1 = new FormatVersionNormalizedEvent(formats.get(0));
    FormatVersionNormalizedEvent event2 = new FormatVersionNormalizedEvent(formats.get(1));

    underTest.on(event1);
    underTest.on(event2);

    Map<String, Object> currentState = underTest.getState();

    assertNotNull(currentState);
    assertTrue((boolean) currentState.get(String.format(NormalizeComponentVersionTask.KEY_FORMAT, "maven2")));
    assertTrue((boolean) currentState.get(String.format(NormalizeComponentVersionTask.KEY_FORMAT, "npm")));
  }
}
