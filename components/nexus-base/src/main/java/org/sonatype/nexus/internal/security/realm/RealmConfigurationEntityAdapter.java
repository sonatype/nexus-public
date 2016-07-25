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
package org.sonatype.nexus.internal.security.realm;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;
import org.sonatype.nexus.security.realm.RealmConfiguration;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link RealmConfiguration} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class RealmConfigurationEntityAdapter
    extends SingletonEntityAdapter<RealmConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("realm")
      .build();

  private static final String P_REALM_NAMES = "realm_names";

  public RealmConfigurationEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REALM_NAMES, OType.EMBEDDEDLIST);
  }

  @Override
  protected RealmConfiguration newEntity() {
    return new RealmConfiguration();
  }

  @Override
  protected void readFields(final ODocument document, final RealmConfiguration entity) {
    List<String> realms = document.field(P_REALM_NAMES, OType.EMBEDDEDLIST);

    entity.setRealmNames(realms);
  }

  @Override
  protected void writeFields(final ODocument document, final RealmConfiguration entity) {
    document.field(P_REALM_NAMES, entity.getRealmNames());
  }
}
