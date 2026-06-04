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
package com.sonatype.nexus.ssl.plugin.tasks;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.logging.task.TaskLogging;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.logging.task.TaskLogType.NEXUS_LOG_ONLY;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

/**
 * Background task (hidden from users) that migrate trusted certificates from key_store_data to trusted_ssl_certificate.
 *
 */
@Lazy
@Scope(SCOPE_PROTOTYPE)
@Component
@TaskLogging(NEXUS_LOG_ONLY)
public class TrustedCertificatesMigrationTask
    extends TaskSupport
    implements Cancelable
{
  private final TrustedCertificateMigrationService trustedCertificateMigrationService;

  @Autowired
  public TrustedCertificatesMigrationTask(final TrustedCertificateMigrationService trustedCertificateMigrationService) {
    this.trustedCertificateMigrationService = checkNotNull(trustedCertificateMigrationService);
  }

  @Override
  protected Void execute() throws Exception {
    trustedCertificateMigrationService.migrate();
    return null;
  }

  @Override
  public String getMessage() {
    return "Migrate trusted certificates";
  }
}
