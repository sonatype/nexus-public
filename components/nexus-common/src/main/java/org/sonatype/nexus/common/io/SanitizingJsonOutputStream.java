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
package org.sonatype.nexus.common.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import static com.fasterxml.jackson.core.JsonTokenId.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sanitizes JSON streamed output by replacing named fields with a specified replacement string. For fields containing
 * nested object or array literals, the replacement is performed recursively on all child elements.
 *
 * @since 3.0
 */
public class SanitizingJsonOutputStream
    extends PipedOutputStream
{
  private static final JsonFactory jsonFactory = new JsonFactory();

  private final JsonGenerator generator;

  private final Thread pipe;

  private IOException ioException;

  /**
   * Constructor.
   */
  public SanitizingJsonOutputStream(
      final OutputStream out,
      final Collection<String> fields,
      final String replacement) throws IOException
  {
    generator = new SanitizingJsonGenerator(jsonFactory.createGenerator(out), fields, replacement);
    PipedInputStream pipedInput = new PipedInputStream(this);

    pipe = new Thread(() -> {
      try (JsonParser parser = jsonFactory.createParser(pipedInput)) {
        parser.nextToken();
        generator.copyCurrentStructure(parser);
      }
      catch (IOException e) {
        ioException = e;
      }
    });

    pipe.start();
  }

  @Override
  public void close() throws IOException {
    super.close();

    try {
      pipe.join();
    }
    catch (InterruptedException e) {
      throw new IOException(e);
    }
    finally {
      generator.close();
    }

    if (ioException != null) {
      throw ioException;
    }
  }

  /**
   * {@link JsonGeneratorDelegate} that performs the actual replacement of fields (and nested fields).
   */
  private static class SanitizingJsonGenerator
      extends JsonGeneratorDelegate
  {
    private final Set<String> fields;

    private final String replacement;

    private int skip;

    /**
     * Constructor.
     */
    public SanitizingJsonGenerator(
        final JsonGenerator delegate,
        final Collection<String> fields,
        final String replacement)
    {
      super(delegate, false);
      this.fields = new HashSet<>(fields);
      this.replacement = checkNotNull(replacement);
    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException {
      if (jp.hasTokenId(ID_FIELD_NAME) && fields.contains(jp.getCurrentName())) {
        skip++;
        super.copyCurrentStructure(jp);
        skip--;
      }
      else {
        super.copyCurrentStructure(jp);
      }
    }

    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException {
      boolean shouldReplace = skip > 0 && !jp.getText().isEmpty();

      if (shouldReplace) {
        writeString(replacement);
      }
      else {
        super.copyCurrentEvent(jp);
      }
    }

    @Override
    protected void _copyCurrentContents(final JsonParser p) throws IOException {
      int depth = 1;
      JsonToken t;
      boolean replaceNext = false;

      // Variation from superclass
      while ((t = p.nextToken()) != null) {
        switch (t.id()) {
          case ID_FIELD_NAME:
            if (fields.contains(p.getCurrentName())) {
              replaceNext = true;
            }
            writeFieldName(p.getCurrentName());
            break;

          case ID_START_ARRAY:
            writeStartArray();
            replaceNext = false;
            ++depth;
            break;

          case ID_START_OBJECT:
            writeStartObject();
            replaceNext = false;
            ++depth;
            break;

          case ID_END_ARRAY:
            writeEndArray();
            if (--depth == 0) {
              return;
            }
            break;
          case ID_END_OBJECT:
            writeEndObject();
            if (--depth == 0) {
              return;
            }
            break;

          case ID_STRING:
            if (replaceNext && !p.getText().isEmpty()) {
              writeString(replacement);
            }
            else if (p.hasTextCharacters()) {
              writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
            }
            else {
              writeString(p.getText());
            }
            break;
          case ID_NUMBER_INT: {
            NumberType n = p.getNumberType();
            if (n == NumberType.INT) {
              writeNumber(p.getIntValue());
            }
            else if (n == NumberType.BIG_INTEGER) {
              writeNumber(p.getBigIntegerValue());
            }
            else {
              writeNumber(p.getLongValue());
            }
            break;
          }
          case ID_NUMBER_FLOAT: {
            NumberType n = p.getNumberType();
            if (n == NumberType.BIG_DECIMAL) {
              writeNumber(p.getDecimalValue());
            }
            else if (n == NumberType.FLOAT) {
              writeNumber(p.getFloatValue());
            }
            else {
              writeNumber(p.getDoubleValue());
            }
            break;
          }
          case ID_TRUE:
            writeBoolean(true);
            break;
          case ID_FALSE:
            writeBoolean(false);
            break;
          case ID_NULL:
            writeNull();
            break;
          case ID_EMBEDDED_OBJECT:
            writeObject(p.getEmbeddedObject());
            break;
          default:
            throw new IllegalStateException("Internal error: unknown current token, " + t);
        }
        if (replaceNext) {
          consume(p);
          replaceNext = false;
        }
      }
    }

    private void consume(final JsonParser p) throws IOException {
      int depth = 1;
      int skip = 0;
      JsonToken t;

      while (skip >= 0 && (t = p.nextToken()) != null) {
        switch (t.id()) {
          case ID_FIELD_NAME:
            writeFieldName(p.getCurrentName());
            ++skip;
            break;
          case ID_START_ARRAY:
            writeStartArray();
            ++depth;
            break;
          case ID_START_OBJECT:
            writeStartObject();
            ++depth;
            break;
          case ID_END_ARRAY:
            writeEndArray();
            if (--depth == 0) {
              return;
            }
            --skip;
            break;
          case ID_END_OBJECT:
            writeEndObject();
            if (--depth == 0) {
              return;
            }
            --skip;
            break;
          case ID_NULL:
            writeNull();
            break;
          case ID_NUMBER_INT:
          case ID_NUMBER_FLOAT:
          case ID_TRUE:
          case ID_FALSE:
          case ID_STRING:
            if (!p.getText().isEmpty()) {
              writeString(replacement);
            }
            else {
              writeString(p.getText());
            }
            --skip;
            break;
          case ID_EMBEDDED_OBJECT:
            writeObject(p.getEmbeddedObject());
            break;
          default:
            throw new IllegalStateException("Internal error: unknown current token, " + t);
        }
      }
    }
  }
}
