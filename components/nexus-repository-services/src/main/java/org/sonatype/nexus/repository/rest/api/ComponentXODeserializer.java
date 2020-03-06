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
package org.sonatype.nexus.repository.rest.api;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Custom {@link JsonDeserializer} for the {@link ComponentXO} to handle the decorator approach
 *
 * @since 3.8
 */
public class ComponentXODeserializer
    extends JsonDeserializer<ComponentXO>
{
  private final ComponentXOFactory componentXOFactory;

  private final ObjectMapper objectMapper;

  private final Set<ComponentXODeserializerExtension> componentXODeserializerExtensions;

  public ComponentXODeserializer(final ComponentXOFactory componentXOFactory,
                                 final ObjectMapper objectMapper,
                                 final Set<ComponentXODeserializerExtension> componentXODeserializerExtensions)
  {
    this.componentXOFactory = checkNotNull(componentXOFactory);
    this.objectMapper = checkNotNull(objectMapper);
    this.componentXODeserializerExtensions = checkNotNull(componentXODeserializerExtensions);
  }

  @Override
  public ComponentXO deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException
  {
    JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);

    ComponentXO componentXO = componentXOFactory.createComponentXO();

    // update the fresh ComponentXO with its own properties
    objectMapper.readerForUpdating(componentXO).readValue(jsonNode);

    // allow each extension to update its properties
    for (ComponentXODeserializerExtension componentXODeserializerExtension : componentXODeserializerExtensions) {
      componentXO = componentXODeserializerExtension.updateComponentXO(componentXO, jsonNode);
    }

    return componentXO;
  }
}
