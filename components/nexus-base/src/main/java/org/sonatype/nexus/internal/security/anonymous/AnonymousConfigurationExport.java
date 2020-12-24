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
package org.sonatype.nexus.internal.security.anonymous;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.supportzip.ExportSecurityData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

/**
 * Write/Read {@link AnonymousConfiguration} data to/from a JSON file.
 *
 * @since 3.29
 */
@Named("anonymousConfigurationExport")
@Singleton
public class AnonymousConfigurationExport
    extends JsonExporter
    implements ExportSecurityData, ImportData
{
  private final AnonymousConfigurationStore anonymousConfigurationStore;

  @Inject
  public AnonymousConfigurationExport(final AnonymousConfigurationStore anonymousConfigurationStore) {
    this.anonymousConfigurationStore = anonymousConfigurationStore;
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export AnonymousConfiguration data to {}", file);
    AnonymousConfiguration configuration = anonymousConfigurationStore.load();
    exportObjectToJson(configuration, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring AnonymousConfiguration data from {}", file);
    Optional<AnonymousConfigurationData> configuration = importObjectFromJson(file, AnonymousConfigurationData.class);
    configuration.ifPresent(anonymousConfigurationStore::save);
  }
}
