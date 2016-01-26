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
package org.sonatype.nexus.orient.entity.marshal;

import com.google.gson.Gson;
import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Google GSON {@link Marshaller}.
 *
 * Can marshall deeply complex objects which do not have no-args constructors, but marshals to a String.
 *
 * @since 3.0
 */
public class GsonMarshaller
  implements Marshaller
{
  private final Gson gson;

  public GsonMarshaller(final Gson gson) {
    this.gson = checkNotNull(gson);
  }

  @Override
  public OType getType() {
    return OType.STRING;
  }

  @Override
  public Object marshall(final Object value) throws Exception {
    return gson.toJson(value);
  }

  @Override
  public <T> T unmarshall(final Object marshalled, final Class<T> type) throws Exception {
    checkNotNull(marshalled);
    checkState(marshalled instanceof String, "Marshalled data must be a String; found: %s", marshalled.getClass());

    return gson.fromJson((String)marshalled, type);
  }
}
