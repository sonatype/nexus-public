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
package org.sonatype.nexus.rapture.internal.state

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService
import org.sonatype.nexus.orient.freeze.FreezeRequest
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType
import org.sonatype.nexus.rapture.StateContributor
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.security.privilege.ApplicationPermission

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.collect.ImmutableMap.of

@Named
@Singleton
class DatabaseStateContributor
    extends ComponentSupport
    implements StateContributor
{
  private static final String STATE_ID = 'db'

  private final DatabaseFreezeService databaseFreezeService

  private final SecurityHelper securityHelper

  @Inject
  DatabaseStateContributor(final DatabaseFreezeService databaseFreezeService, final SecurityHelper securityHelper) {
    this.databaseFreezeService = checkNotNull(databaseFreezeService)
    this.securityHelper = checkNotNull(securityHelper)
  }

  @Override
  Map<String, Object> getState() {
    List<FreezeRequest> requests = databaseFreezeService.getState()
    return of(STATE_ID,
        of(
          'dbFrozen', databaseFreezeService.isFrozen(),
          'system', isSystem(requests),
          'reason', calculateReason(requests)
        )
    )
  }

  String calculateReason(List<FreezeRequest> requests) {
    if (requests.isEmpty() ||
      !securityHelper.allPermitted(new ApplicationPermission("*", Arrays.asList("read")))) {
      return ""
    }

    def userInitiated = requests.find { it.initiatorType == InitiatorType.USER_INITIATED }
    if (userInitiated != null) {
      return "activated by an administrator at ${userInitiated.getTimestamp().toString("yyyy-MM-dd HH:mm:ss ZZ")}"
    } else {
      return "activated by ${requests.size()} running system task(s)"
    }
  }

  boolean isSystem(List<FreezeRequest> requests) {
    return !requests.findAll { it.initiatorType == InitiatorType.SYSTEM }?.isEmpty()
  }
}
