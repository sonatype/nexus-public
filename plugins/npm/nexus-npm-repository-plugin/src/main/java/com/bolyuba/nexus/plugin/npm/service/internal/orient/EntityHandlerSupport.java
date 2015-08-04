/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal.orient;

import java.util.Locale;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EntityHandler} support.
 */
public abstract class EntityHandlerSupport<T>
    implements EntityHandler<T>
{
  private final Class<T> type;

  private final String schemaName;

  private final OClass otype;

  protected EntityHandlerSupport(final Class<T> type, final ODatabaseDocumentTx db) {
    this.type = checkNotNull(type);
    this.schemaName = type.getSimpleName().toLowerCase(Locale.US);
    this.otype = maybeCreateSchema(db);
  }

  @Override
  public String getSchemaName() {
    return schemaName;
  }

  @Override
  public Class<T> getJavaType() {
    return type;
  }

  @Override
  public OClass getOrientType() {
    return otype;
  }

  protected OClass maybeCreateSchema(final ODatabaseDocumentTx db) {
    final OSchema schema = db.getMetadata().getSchema();
    OClass clazz = schema.getClass(getSchemaName());
    if (clazz == null) {
      clazz = schema.createClass(getSchemaName());
      createSchema(schema, clazz);
    }
    else {
      updateSchema(schema, clazz);
    }
    return clazz;
  }

  protected abstract void createSchema(final OSchema schema, OClass clazz);

  protected abstract void updateSchema(final OSchema schema, OClass clazz);

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "type=" + type +
        ", schemaName='" + schemaName + '\'' +
        '}';
  }
}
