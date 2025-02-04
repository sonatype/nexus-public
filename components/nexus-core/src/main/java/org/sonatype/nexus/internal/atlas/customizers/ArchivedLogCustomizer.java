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
package org.sonatype.nexus.internal.atlas.customizers;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.internal.atlas.SupportZipGeneratorImpl;
import org.sonatype.nexus.supportzip.FileContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.OPTIONAL;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.ARCHIVEDLOG;

@Named
@Singleton
public class ArchivedLogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public ArchivedLogCustomizer(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    int archivedLogSize = SupportZipGeneratorImpl.getArchivedLogSize();
    includeArchivedLogs(supportBundle, archivedLogSize);
  }

  private void includeFileIfExists(
      final SupportBundle supportBundle,
      final File file,
      final String prefix,
      final Priority priority)
  {
    if (file != null && file.exists()) {
      log.debug("Including file: {}", file);
      supportBundle.add(
          new FileContentSourceSupport(ARCHIVEDLOG, String.format("%s/%s", prefix, file.getName()), file, priority));
    }
    else {
      log.debug("Skipping non-existent file: {}", file);
    }
  }

  private void includeArchivedLogs(final SupportBundle supportBundle, final int archivedLogSize) {
    List<String> archivedLogList = archivedLogFormatter(archivedLogSize);
    for (String log : archivedLogList) {
      includeFileIfExists(supportBundle, new File(applicationDirectories.getWorkDirectory(), log), "log/archived-logs/",
          OPTIONAL);
    }
  }

  private List<String> archivedLogFormatter(int archivedLogSize) {
    ArrayList<String> archivedLogList = new ArrayList<String>();
    String LOGEXT = ".log.gz";
    LocalDate currentDate = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    for (int i = 0; i <= archivedLogSize; i++) {
      StringBuilder requestLog = new StringBuilder("log/request-");
      StringBuilder auditLog = new StringBuilder("log/audit/audit-");
      StringBuilder nexusLog = new StringBuilder("log/nexus-");
      // need to take the current date and subtract i days from it
      LocalDate previousDate = currentDate.minusDays(i);
      String formattedPreviousDate = previousDate.format(formatter);

      requestLog.append(formattedPreviousDate).append(LOGEXT);
      auditLog.append(formattedPreviousDate).append(LOGEXT);
      nexusLog.append(formattedPreviousDate).append(LOGEXT);

      archivedLogList.add(requestLog.toString());
      archivedLogList.add(auditLog.toString());
      archivedLogList.add(nexusLog.toString());
    }
    return archivedLogList;
  }

}
