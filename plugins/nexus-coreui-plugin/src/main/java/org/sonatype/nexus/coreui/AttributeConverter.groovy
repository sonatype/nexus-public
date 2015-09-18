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
package org.sonatype.nexus.coreui

import javax.inject.Named

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.eclipse.sisu.EagerSingleton

/**
 * Facilities to convert nested maps to/from JSON representation for DTOs.
 * @since 3.0
 */
@Named
@EagerSingleton
class AttributeConverter
{

  private static final ObjectMapper MAPPER = new ObjectMapper()

  private static final ObjectWriter PRETTY_PRINTER = MAPPER.writerWithDefaultPrettyPrinter()

  /**
   * Convert a JSON String representation into a Map.
   * @param attributes
   */
  Map<String, Map<String, Object>> asAttributes(final String attributes) {
    if (!attributes) {
      return null;
    }
    TypeReference<Map<String, Map<String, Object>>> typeRef = new TypeReference<Map<String, Map<String, Object>>>() {}
    return MAPPER.readValue(attributes, typeRef)
  }

  /**
   * Convert a map into a JSON String representation.
   * @param attributes
   */
  String asAttributes(final Map<String, Map<String, Object>> attributes) {
    if (!attributes) {
      return null;
    }
    return PRETTY_PRINTER.writeValueAsString(attributes)
  }
}
