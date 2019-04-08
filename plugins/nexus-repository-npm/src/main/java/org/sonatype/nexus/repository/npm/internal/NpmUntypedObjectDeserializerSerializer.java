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

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.json.UntypedObjectDeserializerSerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link UntypedObjectDeserializer} that is NPM specific by instantly writing out to the provided generator,
 * rather then maintaining references in a map until all values have been deserialized.
 *
 * @since 3.next
 */
public class NpmUntypedObjectDeserializerSerializer
    extends UntypedObjectDeserializerSerializer
{
  private final List<NpmFieldMatcher> matchers;

  public NpmUntypedObjectDeserializerSerializer(final JsonGenerator generator,
                                                final List<NpmFieldMatcher> matchers)
  {
    super(generator);
    this.matchers = checkNotNull(matchers);
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
    String fieldName = parser.getCurrentName();

    for (NpmFieldMatcher matcher : matchers) {
      if (matcher.matches(parser) && matcher.allowDeserializationOnMatched()) {
        // first matcher wins
        return matcher.getDeserializer().deserialize(fieldName, defaultValueDeserialize(parser, context), parser, context, generator);
      }
    }

    return defaultDeserialize(fieldName, parser, context);
  }
}
