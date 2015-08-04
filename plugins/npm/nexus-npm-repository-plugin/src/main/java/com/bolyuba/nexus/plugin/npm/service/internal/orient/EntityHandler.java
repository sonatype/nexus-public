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

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Bridge for OrientDB document and entities.
 */
public interface EntityHandler<T>
{
  /**
   * Returns the name of the schema as used in OrientDB. To be used to construct SQL queries or creating new document
   * instances.
   */
  String getSchemaName();

  /**
   * Returns the entity class handled by this handler.
   */
  Class<T> getJavaType();

  /**
   * Returns the orient class handled by this handler.
   */
  OClass getOrientType();

  /**
   * Translates the entity into Orient's {@link ODocument}.
   */
  ODocument toDocument(T entity, ODocument doc);

  /**
   * Translates Orient's {@link ODocument} to entity.
   */
  T toEntity(ODocument doc);
}
