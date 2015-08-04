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

import java.util.Map;

import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

/**
 * {@link EntityHandler} for {@link PackageRoot} entity.
 */
public class PackageRootHandler
    extends EntityHandlerSupport<PackageRoot>
{
  private final ObjectMapper objectMapper;

  public PackageRootHandler(final ODatabaseDocumentTx db) {
    super(PackageRoot.class, db);
    this.objectMapper = new ObjectMapper(new SmileFactory());
  }

  @Override
  protected void createSchema(final OSchema schema, final OClass clazz) {
    clazz.createProperty("componentId", OType.STRING); // repositoryId:name
    clazz.createProperty("repositoryId", OType.STRING);
    clazz.createProperty("name", OType.STRING);
    clazz.createProperty("description", OType.STRING);
    clazz.createProperty("properties", OType.EMBEDDEDMAP, OType.STRING);
    // TODO: sort out schema for raw?
    clazz.createProperty("raw", OType.LINK); // Using linked "blob" record
    clazz.createIndex(clazz.getName() + ".componentId", INDEX_TYPE.UNIQUE_HASH_INDEX, "componentId");
    clazz.createIndex(clazz.getName() + ".pagedRepositoryId", INDEX_TYPE.UNIQUE, "repositoryId", "@rid");
  }

  @Override
  protected void updateSchema(final OSchema schema, final OClass clazz) {
    if (clazz.getClassIndex(clazz.getName() + ".pagedRepositoryId") == null) {
      clazz.createIndex(clazz.getName() + ".pagedRepositoryId", INDEX_TYPE.UNIQUE, "repositoryId", "@rid");
    }
  }

  @Override
  public ODocument toDocument(final PackageRoot entity, final ODocument doc) {
    try {
      doc.field("componentId", entity.getComponentId());
      doc.field("repositoryId", entity.getRepositoryId());
      doc.field("name", entity.getName());
      doc.field("description", entity.getDescription());
      doc.field("properties", entity.getProperties());
      ORecord rawBytes = doc.field("raw");
      final byte[] buf = objectMapper.writeValueAsBytes(entity.getRaw());
      if (rawBytes == null) {
        rawBytes = new ORecordBytes(buf);
      }
      else {
        rawBytes.clear().fromStream(buf);
      }
      doc.field("raw", rawBytes);
      return doc;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public PackageRoot toEntity(final ODocument doc) {
    try {
      final String repositoryId = doc.field("repositoryId", OType.STRING);
      final ORecordBytes rawBytes = doc.field("raw");
      final Map<String, Object> raw = objectMapper.readValue(rawBytes.toStream(),
          new TypeReference<Map<String, Object>>() {});
      final PackageRoot result = new PackageRoot(repositoryId, raw);
      final Map<String, String> properties = doc.field("properties", OType.EMBEDDEDMAP);
      result.getProperties().putAll(properties);
      return result;
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
