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

import java.io.IOException;

import org.sonatype.goodies.testsupport.TestSupport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.fasterxml.jackson.core.JsonTokenId.ID_NULL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmFieldDeserializerTest
    extends TestSupport
{
  private static final String FIELD_NAME = "test";

  private static final String FIELD_VALUE = "testValue";

  @Mock
  private JsonParser parser;

  @Mock
  private DeserializationContext context;

  @Mock
  private JsonGenerator generator;

  private NpmFieldDeserializer underTest;

  @Before
  public void setUp() {
    underTest = new NpmFieldDeserializer();
  }

  @Test
  public void should_Deserialize_Default_Value() throws IOException {
    Object value = underTest.deserialize(FIELD_NAME, FIELD_VALUE, parser, context, generator);

    assertThat(value, equalTo(value));

    verify(generator).writeFieldName(eq(FIELD_NAME));
    verify(generator).writeObject(eq(FIELD_VALUE));
  }

  @Test
  public void should_Deserialize_Null_Value() throws IOException {
    when(parser.currentTokenId()).thenReturn(ID_NULL);

    Object value = underTest.deserialize(FIELD_NAME, null, parser, context, generator);

    assertThat(value, nullValue());

    verify(generator).writeFieldName(eq(FIELD_NAME));
    verify(generator).writeObject(eq(null));
  }

  @Test
  public void should_Not_Deserialize_Null_Value() throws IOException {
    Object value = underTest.deserialize(FIELD_NAME, null, parser, context, generator);

    assertThat(value, nullValue());

    verify(generator).writeFieldName(eq(FIELD_NAME));
    verify(generator, never()).writeObject(any());
  }

  @Test
  public void should_Deserialize_Exact_Given_Value_By_Default() {
    Object value = underTest.deserializeValue(FIELD_VALUE);

    assertThat(value, equalTo(value));
  }
}
