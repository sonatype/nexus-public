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

import com.fasterxml.jackson.databind.deser.std.MapDeserializer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that acts as a {@link MapDeserializer} but allows the results to be serialized out right away by
 * using a {@link UntypedObjectDeserializerSerializer} for its deserialization of individual objects and arrays.
 */
public class MapDeserializerSerializer
    extends MapDeserializer
{
  public MapDeserializerSerializer(final MapDeserializer rootDeserializer,
                                   final UntypedObjectDeserializerSerializer deserializerSerializer)
  {
    super(checkNotNull(rootDeserializer).getValueType(), checkNotNull(rootDeserializer).getValueInstantiator(), null,
        checkNotNull(deserializerSerializer), null);
  }
}
