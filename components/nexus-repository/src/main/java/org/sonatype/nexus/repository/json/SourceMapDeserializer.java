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

import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Implementation of {@link MapDeserializer} that opens up the "from source" {@link MapDeserializer} constructor.
 *
 * @since 3.17
 */
public class SourceMapDeserializer
    extends MapDeserializer
{
  private SourceMapDeserializer(final MapDeserializer src,
                               final KeyDeserializer keyDeser,
                               final JsonDeserializer<Object> valueDeser,
                               final TypeDeserializer valueTypeDeser,
                               final NullValueProvider nuller,
                               final Set<String> ignorable)
  {
    super(src, keyDeser, valueDeser, valueTypeDeser, nuller, ignorable);
  }

  public static SourceMapDeserializer of(final JavaType mapType,
                                         final ValueInstantiator valueInstantiator,
                                         final JsonDeserializer<Object> deserializer)
  {
    return of(new MapDeserializer(mapType, valueInstantiator, null, deserializer, null));
  }

  public static SourceMapDeserializer of(final MapDeserializer mapDeserializer)
  {
    JsonDeserializer<Object> deserializer = mapDeserializer.getContentDeserializer();
    return new SourceMapDeserializer(mapDeserializer, null, deserializer, null, deserializer, null);
  }
}
