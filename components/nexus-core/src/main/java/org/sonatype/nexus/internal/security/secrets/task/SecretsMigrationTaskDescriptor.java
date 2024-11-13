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
package org.sonatype.nexus.internal.security.secrets.task;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * This task should only be run on systems where {@link SecretsService.SECRETS_MIGRATION_VERSION} is valid, for
 * as it's intended to be invoked in the context of an upgrade step in the same context as the database upgrade we use
 * 1.0 for the availability version.
 */
@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class SecretsMigrationTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "secrets.migration";

  private static final String EXPOSED_FLAG = "${nexus.secrets.migration.exposed:-false}";

  private static final String VISIBLE_FLAG = "${nexus.secrets.migration.visible:-false}";

  @Inject
  public SecretsMigrationTaskDescriptor(
      @Named(EXPOSED_FLAG) final boolean exposed,
      @Named(VISIBLE_FLAG) final boolean visible)
  {
    super(TYPE_ID,
        SecretsMigrationTask.class,
        "Secrets - Migrate secrets",
        visible,
        exposed);
  }
}
