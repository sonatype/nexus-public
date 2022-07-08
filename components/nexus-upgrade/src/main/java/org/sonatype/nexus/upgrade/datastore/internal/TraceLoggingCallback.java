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
package org.sonatype.nexus.upgrade.datastore.internal;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Flyway callback which trace logs the actions being performed.
 *
 * @since 3.29
 */
public class TraceLoggingCallback
    extends BaseCallback
{
  private static final Logger log = LoggerFactory.getLogger(TraceLoggingCallback.class);

  @Override
  public void handle(final Event event, final Context context) {
    // Strict NULL checking as otherwise startup/migration can break.
    String eventID = event != null ? event.getId() : "N/A";
    MigrationInfo migrationInfo = context != null ? context.getMigrationInfo() : null;
    String description = migrationInfo != null ? migrationInfo.getDescription() : "N/A";
    String state = migrationInfo != null ? migrationInfo.getState().toString() : "N/A";
    log.trace("{} Migration Description: \"{}\" State: \"{}\"", eventID, description, state);
  }
}
