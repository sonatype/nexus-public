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
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.END_OBJECT;
import static com.fasterxml.jackson.core.JsonToken.VALUE_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;

/**
 * Decorating {@link ObjectMapper} to allowing merging of {@link InputStream}s.
 *
 * @since 3.16
 */
public class MergeObjectMapper
{
  private static final TypeReference VALUE_TYPE_REF = new TypeReference<HashMap<String, Object>>()
  {
    // NO OP
  };

  private final CustomerMergeObjectMapper objectMapper = new CustomerMergeObjectMapper();

  /**
   * Similar to {@link #merge(List)} but only for a single {@link InputStream}, allowing for the same manner
   * of parsing the output map as the merged ones.
   *
   * @param inputStream {@link InputStream} to parse
   */
  public NestedAttributesMap read(final InputStream inputStream) throws IOException {
    return merge(singletonList(inputStream), null);
  }

  /**
   * Same as {@link #merge(List, Charset)} but no {@link Charset} needed to be provided.
   *
   * @see #merge(List, Charset)
   */
  public NestedAttributesMap merge(final List<InputStream> inputStreams) throws IOException {
    return objectMapper.merge(inputStreams);
  }

  /**
   * Merge the given {@link InputStream}s into a {@link NestedAttributesMap}. The merging is done
   * according the order of the {@link InputStream}s which dictate which values will be considered
   * the dominant and preserved (the last is the most dominant) value.
   *
   * @param inputStreams {@link List} of {@link InputStream}s
   * @param charset      {@link Charset} used for changing from default UTF-8
   * @return NestedAttributesMap
   */
  public NestedAttributesMap merge(final List<InputStream> inputStreams, @Nullable final Charset charset)
      throws IOException
  {
    return objectMapper.merge(inputStreams, charset);
  }

  /**
   * Made available for implementers to have a handle on the result inbetween individual merges.
   *
   * @param result      {@link NestedAttributesMap} result of previous run merge's
   * @param inputStream {@link InputStream} of the latest and therefor most dominant to be added to merge result
   * @param charset     {@link Charset} used for changing from default UTF-8
   */
  protected void merge(final NestedAttributesMap result,
                       final InputStream inputStream,
                       @Nullable final Charset charset)
      throws IOException
  {
    objectMapper.merge(result, inputStream, charset);
  }

  protected void deserialize(final NestedAttributesMapJsonParser parser,
                             final DeserializationContext context,
                             final MapDeserializer rootDeserializer)
      throws IOException
  {
    StdValueInstantiator instantiator = (StdValueInstantiator) rootDeserializer.getValueInstantiator();

    new MapDeserializer(rootDeserializer.getValueType(),
        new NestedAttributesMapStdValueInstantiator(instantiator, parser.getRoot()), null,
        new NestedAttributesMapUntypedObjectDeserializer(parser), null)
        .deserialize(parser, context);
  }

  private class CustomerMergeObjectMapper
      extends ObjectMapper
  {
    private final JavaType valueType = _typeFactory.constructType(VALUE_TYPE_REF);

    private NestedAttributesMap merge(final List<InputStream> inputStreams) throws IOException {
      return merge(inputStreams, null);
    }

    private NestedAttributesMap merge(final List<InputStream> inputStreams, @Nullable final Charset charset)
        throws IOException
    {
      NestedAttributesMap result = new NestedAttributesMap("mergeMap", newHashMap());

      for (InputStream inputStream : inputStreams) {
        merge(result, inputStream, charset);
      }

      return result;
    }

    @SuppressWarnings("unchecked")
    private void merge(final NestedAttributesMap result,
                       final InputStream inputStream,
                       @Nullable final Charset charset)
        throws IOException
    {
      try (NestedAttributesMapJsonParser parser = createParser(result, inputStream, charset)) {

        JsonToken token = _initForReading(parser, valueType);
        DeserializationConfig config = getDeserializationConfig();
        DeserializationContext context = createDeserializationContext(parser, config);

        if (token != VALUE_NULL && token != END_ARRAY && token != END_OBJECT) {
          JsonDeserializer<?> rootDeserializer = _findRootDeserializer(context, valueType);
          deserialize(parser, context, (MapDeserializer) rootDeserializer);

          context.checkUnresolvedObjectId();
        }

        if (config.isEnabled(FAIL_ON_TRAILING_TOKENS)) {
          _verifyNoTrailingTokens(parser, context, valueType);
        }
      }
    }

    private NestedAttributesMapJsonParser createParser(final NestedAttributesMap result,
                                                       final InputStream inputStream,
                                                       @Nullable final Charset charset)
        throws IOException
    {
      if (nonNull(charset)) {
        InputStreamReader reader = new InputStreamReader(inputStream, charset);
        return new NestedAttributesMapJsonParser(_jsonFactory.createParser(reader), result);
      }
      return new NestedAttributesMapJsonParser(_jsonFactory.createParser(inputStream), result);
    }
  }
}
