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
package org.sonatype.nexus.internal.atlas;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.log.SupportZipGeneratorRequest;
import org.sonatype.nexus.supportzip.SupportZipGenerator;
import org.sonatype.nexus.supportzip.SupportZipGenerator.Result;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Action to generate a support ZIP.
 *
 * @since 3.0
 */
@Named
@Command(name = "support-zip", scope = "nexus", description = "Generate a support ZIP")
public class SupportZipAction
    extends ComponentSupport
    implements Action
{
  private final SupportZipGenerator supportZipGenerator;

  @Option(name = "-s", aliases = "--sys-info", description = "Include system information")
  private boolean includeSystemInformation = true;

  @Option(name = "-t", aliases = "--threads", description = "Include thread dump")
  private boolean includeThreadDump = true;

  @Option(name = "-m", aliases = "--metrics", description = "Include metrics")
  private boolean includeMetrics = true;

  @Option(name = "-c", aliases = "--config", description = "Include configuration files")
  private boolean includeConfiguration = true;

  @Option(name = "-e", aliases = "--security", description = "Include security files")
  private boolean includeSecurity = true;

  @Option(name = "-l", aliases = "--log", description = "Include log files")
  private boolean includeLog = true;

  @Option(name = "-a", aliases = "--tasklog", description = "Include task log files")
  private boolean includeTaskLog = true;

  @Option(name = "-au", aliases = "--auditlog", description = "Include audit log files")
  private boolean includeAuditLog = true;

  @Option(name = "-Lf", aliases = "--limit-files", description = "Limit size of included files")
  private boolean includeLimitFileSizes = false;

  @Option(name = "-Lz", aliases = "--limit-zip", description = "Limit size of ZIP")
  private boolean includeLimitZipSize = false;

  @Option(name = "-al", aliases = "--archivedlog", description = "Include 0, 1, 2, 3 days of archived logs")
  private int includeArchivedLogs = 0; // defaults to 0

  @Inject
  public SupportZipAction(final SupportZipGenerator supportZipGenerator) {
    this.supportZipGenerator = checkNotNull(supportZipGenerator);
  }

  @Override
  public Object execute() throws Exception {
    SupportZipGeneratorRequest request = new SupportZipGeneratorRequest();
    request.setSystemInformation(includeSystemInformation);
    request.setThreadDump(includeThreadDump);
    request.setMetrics(includeMetrics);
    request.setConfiguration(includeConfiguration);
    request.setSecurity(includeSecurity);
    request.setLog(includeLog);
    request.setTaskLog(includeTaskLog);
    request.setAuditLog(includeAuditLog);
    request.setLimitFileSizes(includeLimitFileSizes);
    request.setLimitZipSize(includeLimitZipSize);
    request.setArchivedLog(includeArchivedLogs);

    System.out.println("Generating support ZIP...");

    Result result = supportZipGenerator.generate(request);

    System.out.printf("Generated support ZIP: %s%n", result.getFilename());

    return null;
  }
}
