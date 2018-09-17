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
package org.sonatype.nexus.repository.maven.internal.search;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class Maven2ComponentMetadataProducerTest
    extends TestSupport
{
  private Maven2ComponentMetadataProducer underTest;

  @Mock
  private NestedAttributesMap attributes;

  @Mock
  private Component component;

  @Mock
  private NestedAttributesMap childAttributes;

  @Before
  public void setUp() throws Exception {
    underTest = new Maven2ComponentMetadataProducer(emptySet());
  }

  @Test
  public void isReleaseWhenBaseVersionNull() throws Exception {
    when(component.attributes()).thenReturn(attributes);
    when(attributes.child(Maven2Format.NAME)).thenReturn(childAttributes);
    when(childAttributes.get("baseVersion")).thenReturn(null);

    assertThat(underTest.isPrerelease(component, emptySet()), is(false));
  }

  @Test
  public void isReleaseWhenBaseVersionNotSnapshot() throws Exception {
    when(component.attributes()).thenReturn(attributes);
    when(attributes.child(Maven2Format.NAME)).thenReturn(childAttributes);
    when(childAttributes.get("baseVersion")).thenReturn("1.0.0");

    assertThat(underTest.isPrerelease(component, emptySet()), is(false));
  }

  @Test
  public void isPreReleaseWhenBaseVersionEndWithSnapshot() throws Exception {
    when(component.attributes()).thenReturn(attributes);
    when(attributes.child(Maven2Format.NAME)).thenReturn(childAttributes);
    when(childAttributes.get("baseVersion")).thenReturn("1.0.0-SNAPSHOT");

    assertThat(underTest.isPrerelease(component, emptySet()), is(true));
  }
}
