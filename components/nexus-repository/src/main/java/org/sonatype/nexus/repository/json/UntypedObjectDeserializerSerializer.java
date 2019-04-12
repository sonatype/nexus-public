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
package org.sonatype.nexus.repository.json;

import java.io.IOException;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;

import static com.fasterxml.jackson.core.JsonTokenId.ID_NULL;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

/**
 * Class that will deserialize through a {@link JsonParser} as a normal {@link UntypedObjectDeserializer} would
 * but instantly serializes out the values that were deserialized to a given {@link JsonGenerator}.
 *
 * @since 3.16
 */
public class UntypedObjectDeserializerSerializer
    extends UntypedObjectDeserializer
{
  protected final JsonGenerator generator;

  public UntypedObjectDeserializerSerializer(final JsonGenerator generator)
  {
    super(null, null);
    this.generator = checkNotNull(generator);
  }

  /**
   * Overwritten from {@link UntypedObjectDeserializer} allowing the deserialized JSON to be streamed out directly and
   * preventing the deserialized object from being kept in memory.
   *
   * @param parser  {@link JsonParser}
   * @param context {@link DeserializationContext}
   * @return an {@link Object} of any type, if needing to temporary keep it in memory, otherwise null.
   * @throws IOException if unable to properly read and parse given {@link JsonGenerator}.
   */
  @Override
  @Nullable
  public Object deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {
    return defaultDeserialize(parser.getCurrentName(), parser, context);
  }

  /**
   * Overrides the default behaviour to write out values directly to the {@link #generator} and
   * returns null to prevent any references to the deserialized mapped object from being kept in memory.
   *
   * @return null
   */
  @Override
  @Nullable
  protected Object mapObject(final JsonParser parser, final DeserializationContext context) throws IOException {
    generator.writeStartObject();

    super.mapObject(parser, context);

    generator.writeEndObject();

    // allow no reference to mapped object, to prevent memory overuse except for when mapping inside an array.
    return null;
  }

  /**
   * Overrides the default behaviour to write out values directly to the {@link #generator} and
   * returns null to prevent any references to the deserialized mapped array from being kept in memory.
   *
   * @return null
   */
  @Override
  @Nullable
  protected Object mapArray(final JsonParser parser, final DeserializationContext context) throws IOException {
    generator.writeStartArray();

    super.mapArray(parser, context);

    generator.writeEndArray();

    // allow no reference to mapped array, to prevent memory overuse except for when mapping inside an array
    return null;
  }

  protected final Object defaultDeserialize(final String fieldName,
                                            final JsonParser parser,
                                            final DeserializationContext context) throws IOException
  {
    if (nonNull(fieldName)) {
      generator.writeFieldName(fieldName);
    }

    // rely on the real deserialization of jackson's own deserializer
    Object value = defaultValueDeserialize(parser, context);

    if (nonNull(value) || parser.currentTokenId() == ID_NULL) {
      generator.writeObject(value);
    }

    // Depending on whether we are not in an array, this will return null, returning null will prevent high memory usage.
    return value;
  }

  protected final Object defaultValueDeserialize(final JsonParser parser, final DeserializationContext context)
      throws IOException
  {
    // rely on the real deserialization of jackson's own deserializer
    return super.deserialize(parser, context);
  }
}
