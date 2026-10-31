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
package org.sonatype.nexus.supportzip.datastore;

import org.sonatype.nexus.common.time.Time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SecondsSerializer}
 */
public class SecondsSerializerTest
{
  private ObjectMapper mapperWithSerializer() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Time.class, new SecondsSerializer());
    return new ObjectMapper().registerModule(module);
  }

  @Test
  public void testConstructorRegistersHandledType() {
    SecondsSerializer serializer = new SecondsSerializer();
    assertThat(serializer.handledType(), is(Time.class));
  }

  @Test
  public void testSerializeSecondsValue() throws Exception {
    String json = mapperWithSerializer().writeValueAsString(Time.seconds(42));
    assertThat(json, is("42"));
  }

  @Test
  public void testSerializeMinutesConvertedToSeconds() throws Exception {
    String json = mapperWithSerializer().writeValueAsString(Time.minutes(2));
    assertThat(json, is("120"));
  }

  @Test
  public void testSerializeMillisTruncatedToWholeSeconds() throws Exception {
    // 1500 millis -> toSecondsI() truncates to 1 whole second
    String json = mapperWithSerializer().writeValueAsString(Time.millis(1500));
    assertThat(json, is("1"));
  }

  @Test
  public void testSerializeZeroSeconds() throws Exception {
    String json = mapperWithSerializer().writeValueAsString(Time.seconds(0));
    assertThat(json, is("0"));
  }

  @Test
  public void testSerializeWritesNumberToGenerator() throws Exception {
    SecondsSerializer serializer = new SecondsSerializer();
    JsonGenerator jgen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    serializer.serialize(Time.minutes(2), jgen, provider);

    // toSecondsI() returns an int, so the int overload of writeNumber must be the one invoked
    verify(jgen).writeNumber(120);
    Mockito.verifyNoMoreInteractions(jgen);
    Mockito.verifyNoInteractions(provider);
  }

  @Test
  public void testSerializeDirectCallPassesTruncatedSeconds() throws Exception {
    SecondsSerializer serializer = new SecondsSerializer();
    JsonGenerator jgen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    // 1500 millis -> toSecondsI() truncates to the int 1, which is exactly what is written
    serializer.serialize(Time.millis(1500), jgen, provider);

    verify(jgen).writeNumber(1);
    Mockito.verifyNoMoreInteractions(jgen);
    Mockito.verifyNoInteractions(provider);
  }

  @Test
  public void testSerializeSubSecondTruncatesToZero() throws Exception {
    // 999 millis is less than a whole second and truncates toward zero
    String json = mapperWithSerializer().writeValueAsString(Time.millis(999));
    assertThat(json, is("0"));
  }

  @Test(expected = NullPointerException.class)
  public void testSerializeNullValueThrowsNullPointerException() throws Exception {
    SecondsSerializer serializer = new SecondsSerializer();
    JsonGenerator jgen = mock(JsonGenerator.class);
    SerializerProvider provider = mock(SerializerProvider.class);

    serializer.serialize(null, jgen, provider);
  }
}
