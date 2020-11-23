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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/**
 * Parses metadata to extract values without loading the entire model into memory
 *
 * @since 3.next
 */
public class MetadataVersionParser
{
  public static final String VERSIONS = "versions";

  public static List<String> readVersions(final InputStream is) throws IOException {
    try (JsonReader jsonReader = new JsonReader(new InputStreamReader(is))) {
      return extractVersions(jsonReader);
    }
  }

  private static List<String> extractVersions(final JsonReader reader) throws IOException {
    reader.beginObject();
    List<String> versions = new ArrayList<>();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (VERSIONS.equals(name)) {
        versions = readVersions(reader);
      }
      else {
        reader.skipValue();
      }
    }
    reader.endObject();
    return versions;
  }

  private static List<String> readVersions(final JsonReader reader) throws IOException {
    List<String> versions = new ArrayList<>();
    JsonToken peek = reader.peek();
    if (peek.equals(JsonToken.NULL)) {
      reader.skipValue();
      return versions;
    }
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      versions.add(name);
      reader.skipValue();
    }
    reader.endObject();
    return versions;
  }
}
