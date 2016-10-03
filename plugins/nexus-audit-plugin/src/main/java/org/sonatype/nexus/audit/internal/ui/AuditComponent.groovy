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
package org.sonatype.nexus.audit.internal.ui

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.audit.AuditData
import org.sonatype.nexus.audit.AuditRecorder
import org.sonatype.nexus.audit.internal.AuditStore
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.rapture.StateContributor

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

/**
 * Audit {@link DirectComponent}.
 *
 * @since 3.1
 */
@Named
@Singleton
@DirectAction(action = 'audit_Audit')
class AuditComponent
    extends DirectComponentSupport
    implements StateContributor
{
  @Inject
  AuditStore auditStore

  @Inject
  AuditRecorder auditRecorder

  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresPermissions('nexus:audit:read')
  PagedResponse<AuditData> read(final StoreLoadParameters parameters) {
    List<AuditData> data = read(parameters.start, parameters.limit)
    return new PagedResponse<AuditData>(auditStore.approximateSize(), data)
  }

  @RequiresPermissions('nexus:audit:read')
  List<AuditData> read(final long start, final @Nullable Long limit) {
    log.debug "Listing audit records; start=$start limit=$limit"

    List<AuditData> result = []
    def count = 0

    def entries = null
    try {
      entries = auditStore.browse(start, limit)

      for (AuditData data : entries) {
        result << data
        count++
        if (limit != null && limit > 0 && count >= limit) {
          break
        }
      }
    }
    finally {
      entries?.close()
    }

    return result
  }

  /**
   * Clear all audit data.
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @RequiresAuthentication
  @RequiresPermissions('nexus:audit:delete')
  void clear() {
    auditStore.clear()
  }

  //
  // StateContributor
  //

  @Override
  Map<String, Object> getState() {
    return [
        audit: [
            enabled: auditRecorder.enabled
        ]
    ]
  }
}
