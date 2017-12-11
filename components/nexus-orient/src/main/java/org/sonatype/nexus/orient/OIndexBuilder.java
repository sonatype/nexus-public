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
package org.sonatype.nexus.orient;

import java.util.List;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.collate.OCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexDefinition;
import com.orientechnologies.orient.core.index.OIndexDefinitionFactory;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to build an {@link OIndex}.
 *
 * @since 3.3
 */
public class OIndexBuilder
{
  private final OClass type;

  private final String name;

  private final List<String> propertyNames = Lists.newArrayList();

  private final List<OType> propertyTypes = Lists.newArrayList();

  private final INDEX_TYPE indexType;

  private boolean caseInsensitive;

  private boolean ignoreNullValues;

  public OIndexBuilder(final OClass type, final String name, final INDEX_TYPE indexType) {
    this.type = checkNotNull(type);
    this.name = checkNotNull(name);
    this.indexType = checkNotNull(indexType);
  }

  public OIndexBuilder property(final String name, final OType type) {
    propertyNames.add(name);
    propertyTypes.add(type);
    return this;
  }

  public OIndexBuilder caseInsensitive() {
    caseInsensitive = true;
    return this;
  }

  public OIndexBuilder ignoreNullValues() {
    ignoreNullValues = true;
    return this;
  }

  public OIndex build(final ODatabaseDocumentTx db) {
    checkState(!propertyNames.isEmpty(), "At least one property is required");
    checkState(propertyTypes.size() == propertyNames.size(), "A type must be defined for each property");

    List<OCollate> collates = null;
    if (caseInsensitive) {
      collates = Lists.transform(propertyNames, n -> new OCaseInsensitiveCollate());
    }

    ODocument metadata = new ODocument();
    if (ignoreNullValues) {
      metadata.field("ignoreNullValues", true);
    }

    OIndexDefinition indexDefinition = OIndexDefinitionFactory.createIndexDefinition(type, propertyNames, propertyTypes,
        collates, indexType.name(), null);

    return db.getMetadata().getIndexManager().createIndex(name, indexType.name(), indexDefinition,
        type.getPolymorphicClusterIds(), null, metadata.fields() > 0 ? metadata : null);
  }
}
