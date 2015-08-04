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
package org.sonatype.nexus.proxy.attributes;

import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.proxy.attributes.internal.DefaultAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson JSON Attribute marshaller. Part of NEXUS-4628 "alternate" AttributeStorage implementations.
 */
public class JacksonJSONMarshaller
    implements Marshaller
{
  private final ObjectMapper objectMapper;

  public JacksonJSONMarshaller() {
    this.objectMapper = new ObjectMapper();
    // intent was to pretty-print, and this below will turn it on, but unsure
    // abou the perf penalty, and do we really want to fiddle with it now
    // this.objectMapper.configure( SerializationConfig.Feature.INDENT_OUTPUT, true );
  }

  @Override
  public void marshal(final Attributes item, final OutputStream outputStream)
      throws IOException
  {
    final Map<String, String> attrs = new HashMap<String, String>(item.asMap());
    objectMapper.writeValue(outputStream, attrs);
    outputStream.flush();
  }

  @Override
  public Attributes unmarshal(final InputStream inputStream)
      throws IOException, InvalidInputException
  {
    try {
      final Map<String, String> attributesMap =
          objectMapper.readValue(inputStream, new TypeReference<Map<String, String>>()
          {
          });
      return new DefaultAttributes(attributesMap);
    }
    catch (JsonProcessingException e) {
      throw new InvalidInputException("Persisted attribute malformed!", e);
    }
    catch (CharConversionException e) {
      // see NEXUS-5505
      // we trigger invalid input, and the attribute file (or actual Finder file)
      // will be removed from attribute storage. Since we use same name for content
      // and attribute files, this is most we can do here. Downside is that
      // Finder preferences for given folder are lost.
      throw new InvalidInputException("Persisted attribute malformed!", e);
    }
  }

  // ==

  public String toString() {
    return "JacksonJSON";
  }
}
