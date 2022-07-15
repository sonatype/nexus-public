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
package org.sonatype.nexus.repository.maven.upgrade;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import com.google.common.collect.ImmutableMap;

class MavenUpgradeTestSupport
    extends TestSupport
{
  private static final String P_NAME = "name";

  private static final String P_FORMAT = "format";

  protected static final String P_ATTRIBUTES = "attributes";

  private static final String P_BUCKET = "bucket";

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String P_RECIPE_NAME = "recipe_name";

  private static final String REPOSITORY_CLASS = new OClassNameBuilder()
      .type("repository")
      .build();

  protected static final String I_REPOSITORY_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(REPOSITORY_CLASS)
      .property(P_REPOSITORY_NAME)
      .build();

  private static final String BUCKET_CLASS = new OClassNameBuilder()
      .type("bucket")
      .build();

  protected static final String I_BUCKET_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(BUCKET_CLASS)
      .property(P_REPOSITORY_NAME)
      .build();

  private static final String ASSET_CLASS = new OClassNameBuilder()
      .type("asset")
      .build();

  protected static final String I_ASSET_NAME = new OIndexNameBuilder()
      .type(ASSET_CLASS)
      .property(P_NAME)
      .build();

  @Rule
  public DatabaseInstanceRule configDatabase = DatabaseInstanceRule.inMemory("test_config");

  @Rule
  public DatabaseInstanceRule componentDatabase = DatabaseInstanceRule.inMemory("test_component");

  @Before
  public void supportSetup() {
    try (ODatabaseDocumentTx db = configDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();

      // repository
      OClass repositoryType = schema.createClass(REPOSITORY_CLASS);
      repositoryType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setCollate(new OCaseInsensitiveCollate())
          .setMandatory(true)
          .setNotNull(true);
      repositoryType.createProperty(P_RECIPE_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true);
      repositoryType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP);
      repositoryType.createIndex(I_REPOSITORY_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME);
    }

    try (ODatabaseDocumentTx db = componentDatabase.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();

      // bucket
      OClass bucketType = schema.createClass(BUCKET_CLASS);
      bucketType.createProperty(P_REPOSITORY_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true);
      bucketType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP);
      bucketType.createIndex(I_BUCKET_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME);

      // asset
      OClass assetType = schema.createClass(ASSET_CLASS);

      assetType.createProperty(P_NAME, OType.STRING)
          .setMandatory(true)
          .setNotNull(true);
      assetType.createProperty(P_FORMAT, OType.STRING)
          .setMandatory(true)
          .setNotNull(true);
      assetType.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP)
          .setMandatory(true)
          .setNotNull(true);
      assetType.createIndex(I_ASSET_NAME, INDEX_TYPE.UNIQUE, P_NAME);
    }
  }

  protected void repository(final String name, final String recipe) {
    ODocument repository = new ODocument(REPOSITORY_CLASS);
    repository.field(P_REPOSITORY_NAME, name);
    repository.field(P_RECIPE_NAME, recipe);
    repository.save();
  }

  protected void repository(final String name, final String recipe, final Map<String, Map<String, Object>> attributes) {
    ODocument repository = new ODocument(REPOSITORY_CLASS);
    repository.field(P_REPOSITORY_NAME, name);
    repository.field(P_RECIPE_NAME, recipe);
    repository.field(P_ATTRIBUTES, attributes);
    repository.save();
  }

  protected void bucket(final String name) {
    bucket(name, ImmutableMap.of());
  }

  protected void bucket(final String name, final Map<String, Object> attributes) {
    ODocument bucket = new ODocument(BUCKET_CLASS);
    bucket.field(P_REPOSITORY_NAME, name);
    bucket.field(P_ATTRIBUTES, attributes);
    bucket.save();
  }

  protected void asset(
      final OIndex<?> bucketIdx,
      final String repositoryName,
      final String name,
      final String assetKind)
  {
    OIdentifiable idf = (OIdentifiable) bucketIdx.get(repositoryName);
    ODocument asset = new ODocument(ASSET_CLASS);
    asset.field(P_BUCKET, idf);
    asset.field(P_NAME, name);
    asset.field(P_FORMAT, "maven2");
    asset.field(P_ATTRIBUTES, ImmutableMap.of("maven2", ImmutableMap.of("asset_kind", assetKind)));
    asset.save();
  }
}
