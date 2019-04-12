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
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.LogManager;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority.LOW;
import static org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type.AUDITLOG;

/**
 * Adds audit log files to support bundle.
 *
 * @since 3.16
 */
@Named
@Singleton
public class AuditLogCustomizer
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private final LogManager logManager;

  @Inject
  public AuditLogCustomizer(final LogManager logManager) {
    this.logManager = checkNotNull(logManager);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    // add source for nexus.log
    supportBundle.add(new GeneratedContentSourceSupport(AUDITLOG, "log/audit.log", LOW)
    {
      @Override
      protected void generate(final File file) {
        try {
          //didn't bother with try-with-resources as FileUtils.copyInputStreamToFile handles closing the input stream
          InputStream is = logManager.getLogFileStream("audit.log", 0, Long.MAX_VALUE);
          if (is != null) {
            FileUtils.copyInputStreamToFile(is, file);
          }
          else {
            log.debug("Not including missing audit.log file");
          }
        }
        catch (IOException e) {
          log.debug("Unable to include audit.log file", e);
        }
      }
    });
  }
}
