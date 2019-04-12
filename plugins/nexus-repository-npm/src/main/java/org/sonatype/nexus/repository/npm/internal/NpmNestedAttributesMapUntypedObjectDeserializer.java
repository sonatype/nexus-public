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
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.json.NestedAttributesMapJsonParser;
import org.sonatype.nexus.repository.json.NestedAttributesMapUntypedObjectDeserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.overlay;

/**
 * NPM specific {@link NestedAttributesMapUntypedObjectDeserializer} that handles mapping of objects by overlaying
 * the most dominant values over recessive ones (dominant being the last value being deserialized). One exception to
 * that is the NPM versions field, which retains all versions rather then only using the dominant value.
 *
 * @since 3.16
 */
public class NpmNestedAttributesMapUntypedObjectDeserializer
    extends NestedAttributesMapUntypedObjectDeserializer
{
  public NpmNestedAttributesMapUntypedObjectDeserializer(final NestedAttributesMapJsonParser jsonParser) {
    super(jsonParser);
  }

  /**
   * Overridden from parent {@link UntypedObjectDeserializer} to allow specific NPM mapping of the object.
   *
   * @param parser  {@link JsonParser}
   * @param context {@link DeserializationContext}
   * @return Object based on existing children of {@link NestedAttributesMapJsonParser#root} and the currently object
   * that is being mapped.
   */
  @Override
  protected Object mapObject(final JsonParser parser, final DeserializationContext context) throws IOException {
    if (!isDefaultMapping()) {
      // get the current child map from the root map if it existed
      NestedAttributesMap childFromRoot = getChildFromRoot();

      if (nonNull(childFromRoot)) {
        return isMappingField("versions") ?
            mapVersionsObject(parser, context, childFromRoot) :
            mapByOverlaying(parser, context, childFromRoot);
      }
    }

    return super.mapObject(parser, context);
  }

  @SuppressWarnings("unchecked")
  private Object mapByOverlaying(final JsonParser parser,
                                 final DeserializationContext context,
                                 final NestedAttributesMap mapFromRoot) throws IOException
  {
    return overlay(mapFromRoot.backing(), (Map) super.mapObject(parser, context), false);
  }

  private Object mapVersionsObject(final JsonParser parser,
                                   final DeserializationContext context,
                                   final NestedAttributesMap mapFromRoot) throws IOException
  {
    Map<String, Object> recessiveChild = mapFromRoot.backing();
    Map<String, Object> dominantChild = mapVersionsObject(parser, context);

    for (Entry dominantVersion : dominantChild.entrySet()) {
      recessiveChild.put(String.valueOf(dominantVersion.getKey()), dominantVersion.getValue());
    }

    return recessiveChild;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapVersionsObject(final JsonParser parser,
                                                final DeserializationContext context) throws IOException
  {
    try {
      enableDefaultMapping();
      return (Map) super.mapObject(parser, context);
    }
    finally {
      disableDefaultMapping();
    }
  }
}
