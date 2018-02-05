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
package org.sonatype.nexus.script.plugin.internal.orient;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.script.Script;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link Script} entity adapter.
 * 
 * @since 3.0
 */
@Named
@Singleton
public class ScriptEntityAdapter
    extends IterableEntityAdapter<Script>
{
  private static final String P_NAME = "name";

  private static final String P_CONTENT = "content";
  
  private static final String P_TYPE = "type";

  private static final String DB_CLASS = new OClassNameBuilder()
      .type("script")
      .build();

  private static final String I_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_NAME)
      .build();

  public ScriptEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_NAME, OType.STRING).setNotNull(true);
    type.createProperty(P_TYPE, OType.STRING).setNotNull(true);
    type.createProperty(P_CONTENT, OType.STRING).setNotNull(true);

    //ensure name is unique as it serves as the ID for a script
    type.createIndex(I_NAME, INDEX_TYPE.UNIQUE, P_NAME);
  }

  @Override
  protected Script newEntity() {
    return new Script();
  }

  @Override
  protected void readFields(final ODocument document, final Script entity) throws Exception {
    entity.setName(document.field(P_NAME, OType.STRING));
    entity.setType(document.field(P_TYPE, OType.STRING));
    entity.setContent(document.field(P_CONTENT, OType.STRING));
  }

  @Override
  protected void writeFields(final ODocument document, final Script entity) throws Exception {
    document.field(P_NAME, entity.getName());
    document.field(P_TYPE, entity.getType());
    document.field(P_CONTENT, entity.getContent());
  }
}

