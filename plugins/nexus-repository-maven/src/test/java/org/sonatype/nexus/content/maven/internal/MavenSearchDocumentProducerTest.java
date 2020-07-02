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
package org.sonatype.nexus.content.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Constants.SNAPSHOT_VERSION_SUFFIX;

public class MavenSearchDocumentProducerTest
    extends TestSupport
{
  private static final String VERSION_NUMBER = "1.0.0";

  @Mock
  private NestedAttributesMap attributes;

  @Mock
  private NestedAttributesMap childAttributes;

  @Mock
  private FluentComponent component;

  private MavenSearchDocumentProducer underTest;

  @Before
  public void setup() {
    underTest = new MavenSearchDocumentProducer(emptySet());
    when(component.attributes()).thenReturn(attributes);
    when(attributes.child(Maven2Format.NAME)).thenReturn(childAttributes);
  }

  @Test
  public void isPrerelease_shouldBeFalse_when_baseVersionIsNull() {
    assertFalse(underTest.isPrerelease(component));
  }

  @Test
  public void isPrerelease_shouldBeFalse_when_baseVersion_IsNotSnapshot() {
    when(childAttributes.get(P_BASE_VERSION)).thenReturn(VERSION_NUMBER);

    assertFalse(underTest.isPrerelease(component));
  }

  @Test
  public void isPrerelease_shouldBeTrue_when_baseVersion_IsNotSnapshot() {
    when(childAttributes.get(P_BASE_VERSION)).thenReturn(VERSION_NUMBER + SNAPSHOT_VERSION_SUFFIX);

    assertTrue(underTest.isPrerelease(component));
  }
}
