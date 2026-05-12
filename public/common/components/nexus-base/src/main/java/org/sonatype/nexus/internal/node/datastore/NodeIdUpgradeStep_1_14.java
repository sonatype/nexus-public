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
package org.sonatype.nexus.internal.node.datastore;

import java.sql.Connection;
import java.util.Optional;

import jakarta.inject.Inject;

import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Originally migrated the legacy on disk node identifier to the database where it may be used to identify a single
 * node, or a cluster. Now a no-op {@see NodeIdInitializerimpl}
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NodeIdUpgradeStep_1_14
    implements DatabaseMigrationStep
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  public NodeIdUpgradeStep_1_14() {
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.14");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    // no-op
  }
}
