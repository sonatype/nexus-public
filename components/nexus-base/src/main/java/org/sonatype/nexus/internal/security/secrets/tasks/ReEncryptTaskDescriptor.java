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
package org.sonatype.nexus.internal.security.secrets.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import static org.sonatype.nexus.crypto.secrets.SecretsService.SECRETS_MIGRATION_VERSION;

@AvailabilityVersion(from = SECRETS_MIGRATION_VERSION)
@Named(ReEncryptTaskDescriptor.TYPE_ID)
@Singleton
public class ReEncryptTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "security.secrets.re-encrypt";

  public static final String EXPOSED_FLAG = "${nexus.secrets.re-encrypt.task.expose:-false}";

  public static final String VISIBLE_FLAG = "${nexus.secrets.re-encrypt.task.visible:-false}";

  @Inject
  public ReEncryptTaskDescriptor(
      @Named(EXPOSED_FLAG) final boolean exposed,
      @Named(VISIBLE_FLAG) final boolean visible)
  {
    super(TYPE_ID,
        ReEncryptTask.class,
        "Admin - Re-encrypt secrets with the specified key",
        visible,
        exposed,
        true /* request recovery */);
  }
}
