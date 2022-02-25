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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.DependsOn;
import org.sonatype.nexus.common.upgrade.Upgrades;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseInstanceNames;

/**
 * Re-index for update to search normalization format.
 *
 * @since 3.38
 */
@Named
@Singleton
@Upgrades(model = MavenModel.NAME, from = "1.2", to = "1.3")
@DependsOn(model = DatabaseInstanceNames.COMPONENT, version = "1.15", checkpoint = true)
@DependsOn(model = DatabaseInstanceNames.CONFIG, version = "1.9")
public class OrientMavenUpgrade_1_3 // NOSONAR
    extends OrientMavenIndexUpgrade
{
  @Inject
  public OrientMavenUpgrade_1_3(
      @Named(DatabaseInstanceNames.CONFIG) final Provider<DatabaseInstance> configDatabaseInstance,
      @Named(DatabaseInstanceNames.COMPONENT) final Provider<DatabaseInstance> componentDatabaseInstance)
  {
    super(configDatabaseInstance, componentDatabaseInstance);
  }
}
