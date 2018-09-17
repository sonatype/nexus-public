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
package org.sonatype.nexus.repository.npm.internal.search;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.Component;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class NpmComponentMetadataProducerTest
    extends TestSupport
{
  @Mock
  private Component component;

  private NpmComponentMetadataProducer underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new NpmComponentMetadataProducer(emptySet());
  }

  @Test
  public void isReleaseWhenNoDashInVersion() throws Exception {
    when(component.version()).thenReturn("1.0.0");
    assertThat(underTest.isPrerelease(component, emptyList()), is(false));
  }

  @Test
  public void isPreleaseWhenComponentHasDash() throws Exception {
    when(component.version()).thenReturn("1.0.0-pre");
    assertThat(underTest.isPrerelease(component, emptyList()), is(true));
  }
}
