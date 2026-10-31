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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;

/**
 * Decorating {@link ObjectMapper} to allow reading from an {@link InputStream} and writing to
 * an {@link OutputStream} through {@link ObjectMapper}.
 *
 * @since 3.16
 */
public class StreamingObjectMapper
{
  private static final TypeReference<HashMap<String, Object>> RAW_MAP_JSON_TYPE_REF =
      new TypeReference<HashMap<String, Object>>()
      {
        // NO OP
      };

  private final CustomStreamingObjectMapper objectMapper = new CustomStreamingObjectMapper();

  /**
   * Decorator method to {@link ObjectMapper#configure(SerializationFeature, boolean)}
   *
   * return StreamingObjectMapper for chaining.
   */
  public StreamingObjectMapper configure(final SerializationFeature feature, final boolean state) {
    objectMapper.configure(feature, state);
    return this;
  }

  /**
   * Decorator method to {@link ObjectMapper#disable(Feature...)}
   *
   * return StreamingObjectMapper for chaining.
   */
  public StreamingObjectMapper disable(final JsonGenerator.Feature... feature) {
    objectMapper.disable(feature);
    return this;
  }

  /**
   * Decorator method to {@link ObjectMapper#isEnabled(SerializationFeature)}
   */
  public boolean isEnabled(final SerializationFeature feature) {
    return objectMapper.isEnabled(feature);
  }

  /**
   * Read from the given {@link InputStream} and write directly to the given {@link OutputStream}
   * through the internal {@link #objectMapper}.
   *
   * @param input {@link InputStream}
   * @param output {@link OutputStream}
   * @return StreamingObjectMapper for chaining
   * @throws IOException for any issue during write or reading
   */
  public StreamingObjectMapper readAndWrite(final InputStream input, final OutputStream output) throws IOException {
    objectMapper.readAndWrite(input, output);
    return this;
  }

  protected void beforeDeserialize(final JsonGenerator generator) throws IOException {
    // NO OP - let implementer have a say before
  }

  protected void deserializeAndSerialize(
      final JsonParser parser,
      final DeserializationContext context,
      final MapDeserializer rootDeserializer,
      final JsonGenerator generator) throws IOException
  {
    MapDeserializerSerializer.create(rootDeserializer, untypedObjectDeserializer(generator))
        .deserialize(parser, context);
  }

  protected UntypedObjectDeserializerSerializer untypedObjectDeserializer(final JsonGenerator generator) {
    return new UntypedObjectDeserializerSerializer(generator);
  }

  protected void afterDeserialize(final JsonGenerator generator) throws IOException {
    // NO OP - let implementer have a say after
  }

  /**
   * Called by {@link CustomStreamingObjectMapper#_serializerProvider} when no cached provider
   * exists yet for this thread. Subclasses may store the newly allocated provider for reuse.
   * The default implementation is a no-op.
   * <p>
   * Override in subclasses that maintain a per-thread provider cache (e.g. for shared singletons).
   */
  protected void onSerializerProviderCreated(final DefaultSerializerProvider provider) {
    // NO OP
  }

  /**
   * Returns the per-thread cached {@link DefaultSerializerProvider}, or {@code null} if none.
   * Return {@code null} to let Jackson allocate a fresh one (which will then be passed to
   * {@link #onSerializerProviderCreated}).
   * <p>
   * Override in subclasses that maintain a per-thread provider cache (e.g. for shared singletons).
   */
  protected DefaultSerializerProvider threadLocalSerializerProvider() {
    return null;
  }

  /**
   * Called in {@code finally} at the end of each {@link #readAndWrite} call.
   * Override to clean up per-request state (e.g. remove ThreadLocal entries).
   */
  protected void onReadAndWriteComplete() {
    // NO OP
  }

  private class CustomStreamingObjectMapper
      extends ObjectMapper
  {
    private final JavaType valueType = _typeFactory.constructType(RAW_MAP_JSON_TYPE_REF);

    private final JsonFactory jsonFactory;

    private CustomStreamingObjectMapper() {
      this.jsonFactory = new JsonFactory(this);
    }

    @Override
    protected DefaultSerializerProvider _serializerProvider(final SerializationConfig config) {
      DefaultSerializerProvider cached = threadLocalSerializerProvider();
      if (cached != null) {
        return cached;
      }
      DefaultSerializerProvider provider = super._serializerProvider(config);
      onSerializerProviderCreated(provider);
      return provider;
    }

    private void readAndWrite(final InputStream input, final OutputStream output) throws IOException {
      try (JsonParser parser = new CurrentPathJsonParser(_jsonFactory.createParser(input))) {

        JsonToken token = _initForReading(parser, valueType);
        DeserializationConfig config = getDeserializationConfig();
        DeserializationContext context = createDeserializationContext(parser, config);

        if (token != VALUE_NULL && token != END_ARRAY && token != END_OBJECT) {

          try (JsonGenerator generator = jsonFactory.createGenerator(output)) {

            generator.writeStartObject();

            beforeDeserialize(generator);

            JsonDeserializer<?> rootDeserializer = _findRootDeserializer(context, valueType);
            deserializeAndSerialize(parser, context, (MapDeserializer) rootDeserializer, generator);

            afterDeserialize(generator);

            context.checkUnresolvedObjectId();
          }
        }

        if (config.isEnabled(FAIL_ON_TRAILING_TOKENS)) {
          _verifyNoTrailingTokens(parser, context, valueType);
        }
      }
      finally {
        onReadAndWriteComplete();
      }
    }
  }
}
