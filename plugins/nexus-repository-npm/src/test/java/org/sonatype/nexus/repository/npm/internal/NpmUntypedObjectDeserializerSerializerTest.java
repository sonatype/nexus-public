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
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.json.CurrentPathJsonParser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmUntypedObjectDeserializerSerializerTest
    extends TestSupport
{
  private final static String FIELD_NAME = "author";

  private final static String FIELD_VALUE = "shakespeare";

  @Mock
  private JsonGenerator generator;

  @Mock
  private CurrentPathJsonParser parser;

  @Mock
  private DeserializationContext context;

  @Mock
  private NpmFieldMatcher fieldMatcher;

  @Mock
  private NpmFieldDeserializer fieldDeserializer;

  private List<NpmFieldMatcher> fieldMatchers;

  private NpmUntypedObjectDeserializerSerializer underTest;

  @Before
  public void setUp() throws IOException {
    fieldMatchers = newArrayList();
    underTest = new NpmUntypedObjectDeserializerSerializer(generator, fieldMatchers);

    when(parser.getCurrentName()).thenReturn(FIELD_NAME);
    when(context.handleUnexpectedToken(eq(Object.class), eq(parser))).thenReturn(FIELD_VALUE);
    when(fieldMatcher.getDeserializer()).thenReturn(fieldDeserializer);
    when(fieldMatcher.allowDeserializationOnMatched()).thenReturn(true);
  }

  @Test
  public void serializes_During_Deserialization() throws IOException {
    Object deserializedValue = underTest.deserialize(parser, context);

    assertThat(deserializedValue, equalTo(FIELD_VALUE));
    verify(generator).writeFieldName(eq(FIELD_NAME));
    verify(generator).writeObject(eq(FIELD_VALUE));
  }

  @Test
  public void deserialize_With_Matchers() throws IOException {
    when(parser.getCurrentName()).thenReturn(FIELD_NAME);
    when(fieldMatcher.matches(parser)).thenReturn(true);

    fieldMatchers.add(fieldMatcher);

    underTest.deserialize(parser, context);

    verify(fieldMatcher).matches(parser);
    verify(fieldMatcher).getDeserializer();
    verify(fieldMatcher).allowDeserializationOnMatched();
    verify(fieldDeserializer).deserialize(eq(FIELD_NAME), eq(FIELD_VALUE), eq(parser), eq(context), eq(generator));
  }

  @Test
  public void deserialize_With_Matchers_Prevented_If_Not_Allowed() throws IOException {
    when(parser.getCurrentName()).thenReturn(FIELD_NAME);
    when(fieldMatcher.matches(parser)).thenReturn(true);
    when(fieldMatcher.allowDeserializationOnMatched()).thenReturn(false);

    fieldMatchers.add(fieldMatcher);

    underTest.deserialize(parser, context);

    verify(fieldMatcher).matches(parser);
    verify(fieldMatcher).allowDeserializationOnMatched();
    verify(fieldMatcher, never()).getDeserializer();
    verify(fieldDeserializer, never()).deserialize(eq(FIELD_NAME), eq(FIELD_VALUE), eq(parser), eq(context), eq(generator));
  }
}
