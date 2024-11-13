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
package org.sonatype.nexus.internal.datastore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.supportzip.ExportConfigData;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Add {@link SupportBundle} to export {@link Type#CONFIG} data to serialized files.
 *
 * @since 3.29
 */
@Named
@Singleton
public class ConfigDatabase
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  public static final String WORK_PATH = "db";

  private static final Path PATH = Paths.get("work", WORK_PATH);

  private static final String FILE_SUFFIX = ".json";

  private final Map<String, ExportConfigData> exportDataByName;

  @Inject
  public ConfigDatabase(final Map<String, ExportConfigData> exportDataByName) {
    this.exportDataByName = checkNotNull(exportDataByName);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    for (Entry<String, ExportConfigData> exporterEntry : exportDataByName.entrySet()) {
      supportBundle.add(getExporter(exporterEntry.getKey() + FILE_SUFFIX, exporterEntry.getValue()));
    }
  }

  private GeneratedContentSourceSupport getExporter(final String fileName, final ExportConfigData exporter) {
    return new GeneratedContentSourceSupport(Type.CONFIG, PATH.resolve(fileName).toString())
    {
      @Override
      protected void generate(final File file) throws Exception {
        exporter.export(file);
      }
    };
  }

}
