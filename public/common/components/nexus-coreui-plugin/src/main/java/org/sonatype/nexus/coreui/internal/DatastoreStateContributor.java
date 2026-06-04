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
package org.sonatype.nexus.coreui.internal;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.rapture.StateContributor;

import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_IS_POSTGRESQL;

@Component
public class DatastoreStateContributor
    implements StateContributor
{
  private final DatabaseCheck dbCheck;

  private Map<String, Object> state;

  @Autowired
  public DatastoreStateContributor(final DatabaseCheck dbCheck) {
    this.dbCheck = checkNotNull(dbCheck);
  }

  @Override
  public Map<String, Object> getState() {
    if (state == null) {
      state = Map.of(
          "nexus.datastore.enabled", true,
          "nexus.datastore.developer", false,
          DATASTORE_IS_POSTGRESQL, dbCheck.isPostgresql());
    }
    return state;
  }
}
