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
package org.sonatype.nexus.repository.config;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.FieldCopier;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link Configuration} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ConfigurationEntityAdapter
    extends IterableEntityAdapter<Configuration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .prefix("repository")
      .type(Configuration.class)
      .build();

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String P_RECIPE_NAME = "recipe_name";

  private static final String P_ONLINE = "online";

  private static final String P_ATTRIBUTES = "attributes";

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .build();

  public ConfigurationEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_RECIPE_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ONLINE, OType.BOOLEAN)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP);
    type.createIndex(I_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME)
        .getDefinition().setCollate(new OCaseInsensitiveCollate());
  }

  @Override
  protected Configuration newEntity() {
    return new Configuration();
  }

  @Override
  protected void readFields(final ODocument document, final Configuration entity) {
    String recipeName = document.field(P_RECIPE_NAME, OType.STRING);
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    Boolean online = document.field(P_ONLINE, OType.BOOLEAN);
    Map<String, Map<String, Object>> attributes = document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);

    // deeply copy attributes to divorce from document
    attributes = FieldCopier.copyIf(attributes);

    entity.setRecipeName(recipeName);
    entity.setRepositoryName(repositoryName);
    entity.setOnline(online);
    entity.setAttributes(attributes);
  }

  @Override
  protected void writeFields(final ODocument document, final Configuration entity) {
    document.field(P_RECIPE_NAME, entity.getRecipeName());
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_ONLINE, entity.isOnline());
    document.field(P_ATTRIBUTES, entity.getAttributes());
  }
}
