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

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.supportzip.ExportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

/**
 * Write/Read {@link Configuration} data to/from a JSON file.
 *
 * @since 3.next
 */
@Named("configurationExport")
@Singleton
public class ConfigurationExport
    extends JsonExporter
    implements ExportData
{
  private final ConfigurationStore configurationStore;

  @Inject
  public ConfigurationExport(final ConfigurationStore configurationStore) {
    this.configurationStore = configurationStore;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export Configuration data to {}", file);
    List<Configuration> configurations = configurationStore.list();
    exportToJson(configurations, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring Configuration data from {}", file);
    importFromJson(file, ConfigurationData.class).forEach(configurationStore::create);
  }
}
