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

import java.util.List;

import org.sonatype.nexus.repository.json.MapDeserializerSerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.deser.std.MapDeserializer;

/**
 * NPM Specialized {@link MapDeserializerSerializer} that uses a {@link NpmUntypedObjectDeserializerSerializer} for
 * it object deserialization and serializing out.
 *
 * @since 3.16
 */
public class NpmMapDeserializerSerializer
    extends MapDeserializerSerializer
{
  public NpmMapDeserializerSerializer(final MapDeserializer rootDeserializer,
                                      final JsonGenerator generator,
                                      final List<NpmFieldMatcher> matchers)
  {
    super(rootDeserializer, new NpmUntypedObjectDeserializerSerializer(generator, matchers));
  }
}
