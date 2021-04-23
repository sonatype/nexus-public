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
package org.sonatype.nexus.internal.orient;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;

/**
 * {@link SingletonEntityAdapter} to store our single {@link DeploymentIdentifier} record.
 *
 * @since 3.6.1
 */
@FeatureFlag(name = ORIENT_ENABLED)
@Named
@Singleton
class OrientDeploymentIdentifierEntityAdapter
    extends SingletonEntityAdapter<DeploymentIdentifier>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("deploymentidentifier")
      .build();

  static final String P_ID = "id";

  static final String P_ALIAS = "alias";

  public OrientDeploymentIdentifierEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_ID, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ALIAS, OType.STRING)
        .setMandatory(false)
        .setNotNull(false);
  }

  @Override
  protected DeploymentIdentifier newEntity() {
    return new DeploymentIdentifier();
  }

  @Override
  protected void readFields(final ODocument document, final DeploymentIdentifier entity) {
    entity.setId(document.field(P_ID, OType.STRING))
        .setAlias(document.field(P_ALIAS, OType.STRING));
  }

  @Override
  protected void writeFields(final ODocument document, final DeploymentIdentifier entity) {
    document.field(P_ID, entity.getId());
    document.field(P_ALIAS, entity.getAlias());
  }
}
