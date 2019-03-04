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

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link UntypedObjectDeserializer} with using a {@link NestedAttributesMap} as a root map to
 * get existing deserialized references from.
 *
 * @since 3.next
 */
public class NestedAttributesMapUntypedObjectDeserializer
    extends UntypedObjectDeserializer
{
  private NestedAttributesMapJsonParser jsonParser;

  public NestedAttributesMapUntypedObjectDeserializer(final NestedAttributesMapJsonParser jsonParser) {
    super(null, null); // using this null, null constructor like the default deprecated one
    this.jsonParser = checkNotNull(jsonParser);
  }

  /**
   * Overridden to mark when we are mapping inside an array. The marker is available for internal checking.
   */
  @Override
  protected Object mapArray(final JsonParser parser, final DeserializationContext context) throws IOException {
    try {
      markMappingInsideArray();
      return super.mapArray(parser, context);
    }
    finally {
      unMarkMappingInsideArray();
    }
  }

  protected NestedAttributesMap getChildFromRoot() {
    return jsonParser.getChildFromRoot();
  }

  protected boolean isMappingField(final String name) {
    return jsonParser.currentPath().endsWith(name);
  }

  protected void markMappingInsideArray() {
    jsonParser.markMappingInsideArray();
  }

  protected void unMarkMappingInsideArray() {
    jsonParser.unMarkMappingInsideArray();
  }

  protected boolean isDefaultMapping() {
    return jsonParser.isDefaultMapping();
  }

  protected void enableDefaultMapping() {
    jsonParser.enableDefaultMapping();
  }

  protected void disableDefaultMapping() {
    jsonParser.disableDefaultMapping();
  }
}
