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
package org.sonatype.nexus.audit.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Upgrade step to move old journaled audit events to the primary cluster.
 *
 * @since 3.2
 */
@Named
@Singleton
@Upgrades(model = AuditDatabase.NAME, from = "1.0", to = "1.1")
public class AuditDatabaseUpgrade_1_1
    extends ComponentSupport
    implements Upgrade
{
  private final Provider<DatabaseInstance> databaseInstance;

  @Inject
  public AuditDatabaseUpgrade_1_1(@Named(AuditDatabase.NAME) final Provider<DatabaseInstance> databaseInstance) {
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void apply() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      OClass type = schema.getClass("audit_data");
      if (type != null) {
        // is there more than one data cluster?
        int[] clusterIds = type.getClusterIds();
        if (clusterIds.length > 1) {

          // reset default to primary cluster
          int primaryClusterId = clusterIds[0];
          type.setDefaultClusterId(primaryClusterId);

          log.info("Moving events to primary cluster {}", db.getClusterNameById(primaryClusterId));

          int moveCount = 0;

          // move all records to primary cluster
          for (ODocument document : db.browseClass(type.getName())) {
            ORID rid = document.getIdentity();
            if (rid.getClusterId() != primaryClusterId) {
              db.save(new ODocument(document.toStream()));
              db.delete(rid);
              moveCount++;
            }
          }

          log.info("Moved {} events", moveCount);

          // delete all secondary clusters
          for (int clusterId : clusterIds) {
            if (clusterId != primaryClusterId) {
              log.info("Dropping secondary cluster {}", db.getClusterNameById(clusterId));
              db.dropCluster(clusterId, false);
            }
          }
        }
      }
    }
  }
}
