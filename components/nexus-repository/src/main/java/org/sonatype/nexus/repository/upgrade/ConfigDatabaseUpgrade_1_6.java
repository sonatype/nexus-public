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
package org.sonatype.nexus.repository.upgrade;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;
import org.sonatype.nexus.orient.DatabaseUpgradeSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.DatabaseInstanceNames.CONFIG;

/**
 * Deletes broken Component Selectors
 *
 * @since 3.16
 */
@Named
@Singleton
@Upgrades(model = DatabaseInstanceNames.CONFIG, from = "1.5", to = "1.6")
public class ConfigDatabaseUpgrade_1_6 // NOSONAR
    extends DatabaseUpgradeSupport
{

  private static final String SELECTOR_CLASS_NAME = new OClassNameBuilder().prefix("selector").type("selector").build();

  private final Provider<DatabaseInstance> databaseInstance;

  @Inject
  public ConfigDatabaseUpgrade_1_6(@Named(CONFIG) final Provider<DatabaseInstance> databaseInstance)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
  }

  @Override
  public void apply() {
    try (ODatabaseDocumentTx db = databaseInstance.get().connect()) {
      Iterable<ODocument> selectorDocuments = db.browseClass(SELECTOR_CLASS_NAME);
      selectorDocuments.forEach(selector -> {
        try {
          selector.deserializeFields();
        }
        catch (Exception e) {
          log.debug("Deleting broken record due to exception during deserialization", e);

          deleteBrokenRecord(db, selector);

          log.info("Deleted broken content selector {} ", selector.getIdentity().toString());
        }
      });
    }
  }

  private void deleteBrokenRecord(ODatabaseDocumentTx db, ODocument selector) {
    // HACK to get around inability to directly delete documents that cannot be deserialized
    // from OrientDB - see https://github.com/orientechnologies/orientdb/issues/6190
    ODocument replacement = new ODocument();
    replacement.field("name", (String) selector.field("name"));
    replacement.field("type", (String) selector.field("type"));
    replacement.field("description", (String) selector.field("description"));
    replacement.field("attributes", new HashMap<String, String>());
    ORecordInternal.setVersion(replacement, selector.getVersion());
    ORecordInternal.setIdentity(replacement, (ORecordId) selector.getIdentity());
    db.save(replacement);
    db.delete(replacement.getRecord().getIdentity());
  }
}
