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
package org.sonatype.nexus.internal.atlas

import javax.inject.Inject
import javax.inject.Named

import org.sonatype.nexus.supportzip.SupportZipGenerator

import org.apache.karaf.shell.api.action.Action
import org.apache.karaf.shell.api.action.Command
import org.apache.karaf.shell.api.action.Option

/**
 * Action to generate a support ZIP.
 *
 * @since 3.0
 */
@Named
@Command(name = 'support-zip', scope = 'nexus', description = 'Generate a support ZIP')
class SupportZipAction
    implements Action
{
  @Inject
  SupportZipGenerator supportZipGenerator

  @Option(name='-s', aliases = '--sys-info', description = 'Include system information')
  boolean systemInformation = true

  @Option(name='-t', aliases = '--threads', description = 'Include thread dump')
  boolean threadDump = true

  @Option(name='-m', aliases = '--metrics', description = 'Include metrics')
  boolean metrics = true

  @Option(name='-c', aliases = '--config', description = 'Include configuration files')
  boolean configuration = true

  @Option(name='-e', aliases = '--security', description = 'Include security files')
  boolean security = true

  @Option(name='-l', aliases = '--log', description = 'Include log files')
  boolean log = true

  @Option(name='-a', aliases = '--tasklog', description = 'Include task log files')
  boolean taskLog = true

  @Option(name='-au', aliases = '--auditlog', description = 'Include audit log files')
  boolean auditLog = true

  @Option(name='-Lf', aliases = '--limit-files', description = 'Limit size of included files')
  boolean limitFileSizes = false

  @Option(name='-Lz', aliases = '--limit-zip', description = 'Limit size of ZIP')
  boolean limitZipSize = false

  @Override
  public Object execute() throws Exception {
    def request = new SupportZipGenerator.Request(
        systemInformation: systemInformation,
        threadDump: threadDump,
        metrics: metrics,
        configuration: configuration,
        security: security,
        log: log,
        taskLog: taskLog,
        auditLog: auditLog,
        limitFileSizes: limitFileSizes,
        limitZipSize: limitZipSize
    )

    println 'Generating support ZIP...'

    def result = supportZipGenerator.generate(request)

    println "Generated support ZIP: ${result.file}"

    return null
  }
}
