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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmMapDeserializerSerializerTest
    extends TestSupport
{
  @Mock
  private MapDeserializer rootDeserializer;

  @Mock
  private ValueInstantiator valueInstantiator;

  @Mock
  private JsonGenerator generator;

  @Before
  public void setUp() {
    when(rootDeserializer.getValueInstantiator()).thenReturn(valueInstantiator);
    when(valueInstantiator.canCreateUsingDefault()).thenReturn(true);

    new NpmMapDeserializerSerializer(rootDeserializer, generator, emptyList());
  }

  @Test
  public void should_Use_ValueType_From_RootDeserializer() {
    verify(rootDeserializer).getValueType();
  }

  @Test
  public void should_Use_ValueInstantiator_From_RootDeserializer() {
    verify(rootDeserializer).getValueInstantiator();
  }
}
