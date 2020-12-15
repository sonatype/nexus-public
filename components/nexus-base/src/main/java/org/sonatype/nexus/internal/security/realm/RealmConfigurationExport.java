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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.supportzip.ExportSecurityData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

/**
 * Write/Read {@link RealmConfiguration} data to/from a JSON file.
 *
 * @since 3.29
 */
@Named("realmConfigurationExport")
@Singleton
public class RealmConfigurationExport
    extends JsonExporter
    implements ExportSecurityData, ImportData
{
  private final RealmConfigurationStoreImpl configuration;

  @Inject
  public RealmConfigurationExport(final RealmConfigurationStoreImpl configuration) {
    this.configuration = configuration;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export RealmConfiguration data to {}", file);
    exportObjectToJson(configuration.load(), file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring RealmConfiguration data from {}", file);
    Optional<RealmConfigurationData> realmConfiguration = importObjectFromJson(file, RealmConfigurationData.class);
    realmConfiguration.ifPresent(configuration::save);
  }
}
