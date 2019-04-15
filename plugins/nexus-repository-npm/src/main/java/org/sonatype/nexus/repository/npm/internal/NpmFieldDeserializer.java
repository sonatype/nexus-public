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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import static com.fasterxml.jackson.core.JsonTokenId.ID_NULL;
import static java.util.Objects.nonNull;

/**
 * A NPM specialized deserializer for an individual field.
 *
 * @since 3.16
 */
public class NpmFieldDeserializer
{
  /**
   * Deserialize object based on a default writing out of the field name and deserialized value. Implementers of this
   * class should override this to provide their own way of deserializing.
   *
   * @param fieldName    exact name of field as found by {@link JsonParser}
   * @param defaultValue {@link Object} that will provide the actual value to deserialize
   * @param parser       {@link JsonParser}
   * @param context      {@link DeserializationContext}
   * @param generator    {@link JsonGenerator}
   * @return Object deserialized through this field deserializer
   * @throws IOException from potential generating issues
   */
  public Object deserialize(final String fieldName,
                            final Object defaultValue,
                            final JsonParser parser,
                            final DeserializationContext context,
                            final JsonGenerator generator) throws IOException
  {
    generator.writeFieldName(fieldName);

    Object value;

    try {
      value = deserializeValue(defaultValue);
    }
    catch (RuntimeException e) {
      // handle the most likely exception from deserializing and throw as a normal deserializer would
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }

      throw e;
    }

    if (nonNull(value) || parser.currentTokenId() == ID_NULL) {
      generator.writeObject(value);
    }

    return value;
  }

  /**
   * Method for explicitly deserilizing the value of supplied deserialized object.
   */
  protected Object deserializeValue(final Object defaultValue) {
    return defaultValue;
  }
}
