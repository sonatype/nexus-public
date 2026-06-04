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
package org.sonatype.nexus.internal.security.apikey.task;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@AvailabilityVersion(from = "1.1")
@Component
public class ReEncryptPrincipalsTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String TYPE_ID = "reencrypt.principals";

  private static final String EXPOSED_FLAG_VALUE = "${nexus.reencrypt.principals.exposed:false}";

  private static final String VISIBLE_FLAG_VALUE = "${nexus.reencrypt.principals.visible:false}";

  @Autowired
  public ReEncryptPrincipalsTaskDescriptor(
      @Value(EXPOSED_FLAG_VALUE) final boolean exposed,
      @Value(VISIBLE_FLAG_VALUE) final boolean visible)
  {
    super(TYPE_ID,
        ReEncryptPrincipalsTask.class,
        "Re-encryption Principals",
        exposed,
        visible);
  }
}
