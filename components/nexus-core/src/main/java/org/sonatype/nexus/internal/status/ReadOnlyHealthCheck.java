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
package org.sonatype.nexus.internal.status;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FreezeRequest;
import org.sonatype.nexus.common.app.FreezeService;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * Health check that warns when the application is read-only.
 *
 * @since 3.16
 */
@Named("Read-Only Detector")
@Singleton
public class ReadOnlyHealthCheck
    extends HealthCheckComponentSupport
{
  private final FreezeService freezeService;

  @Inject
  public ReadOnlyHealthCheck(final FreezeService freezeService) {
    this.freezeService = checkNotNull(freezeService);
  }

  @Override
  protected Result check() {
    return freezeService.currentFreezeRequests()
        .stream()
        .findFirst()
        .map(this::describe)
        .map(Result::unhealthy)
        .orElse(Result.healthy());
  }

  private String describe(final FreezeRequest request) {
    String description = "Made read-only by: " + request.frozenBy().orElse("SYSTEM");
    if (!isBlank(request.reason())) {
      description += ", reason: " + request.reason();
    }
    return description;
  }
}
