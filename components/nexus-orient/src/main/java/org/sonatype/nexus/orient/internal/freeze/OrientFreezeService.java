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
package org.sonatype.nexus.orient.internal.freeze;

import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.internal.status.OrientStatusHealthCheckStore;
import org.sonatype.nexus.security.SecurityHelper;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.transform;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.SYSTEM;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.USER_INITIATED;

/**
 * Orient implementation of common {@link FreezeService}.
 *
 * @since 3.21
 */
@Named
@Singleton
@Priority(Integer.MAX_VALUE)
public class OrientFreezeService
    extends ComponentSupport
    implements FreezeService
{
  private static final String ANONYMOUS = "anonymous";

  private final DatabaseFreezeService databaseFreezeService;

  private final OrientStatusHealthCheckStore statusHealthCheckStore;

  private final SecurityHelper securityHelper;

  @Inject
  public OrientFreezeService(final DatabaseFreezeService databaseFreezeService,
                             final OrientStatusHealthCheckStore statusHealthCheckStore,
                             final SecurityHelper securityHelper)
  {
    this.databaseFreezeService = checkNotNull(databaseFreezeService);
    this.statusHealthCheckStore = checkNotNull(statusHealthCheckStore);
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  public void requestFreeze(final String reason) {
    FreezeRequest request = databaseFreezeService.requestFreeze(USER_INITIATED, currentUserId());
    checkState(request != null, "Freeze request rejected");
  }

  @Override
  public void cancelFreeze() {
    checkState(databaseFreezeService.releaseUserInitiatedIfPresent(), "No such freeze request");
  }

  @Override
  public void taskRequestFreeze(final String token, final String reason) {
    FreezeRequest request = databaseFreezeService.requestFreeze(SYSTEM, token);
    checkState(request != null, "Freeze request rejected");
  }

  @Override
  public void taskCancelFreeze(final String token) {
    FreezeRequest originalRequest = databaseFreezeService.getState().stream()
        .filter(r -> token.equals(r.getInitiatorId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No such freeze request"));

    databaseFreezeService.releaseRequest(originalRequest);
  }

  @Override
  public List<org.sonatype.nexus.common.app.FreezeRequest> cancelAllFreezeRequests() {
    return transform(databaseFreezeService.releaseAllRequests(), this::adapt);
  }

  @Override
  public boolean isFrozen() {
    return databaseFreezeService.isFrozen();
  }

  @Override
  public List<org.sonatype.nexus.common.app.FreezeRequest> currentFreezeRequests() {
    return transform(databaseFreezeService.getState(), this::adapt);
  }

  @Override
  public void checkReadable(final String errorMessage) {
    statusHealthCheckStore.checkReadable(errorMessage);
  }

  @Override
  public void checkWritable(final String errorMessage) {
    if (isFrozen()) {
      throw new FrozenException(errorMessage);
    }
    statusHealthCheckStore.checkWritable(errorMessage);
  }

  private String currentUserId() {
    return ofNullable(securityHelper.subject().getPrincipal()).map(Object::toString).orElse(ANONYMOUS);
  }

  private org.sonatype.nexus.common.app.FreezeRequest adapt(final FreezeRequest request) {

    String token;
    String userId;
    if (USER_INITIATED == request.getInitiatorType()) {
      userId = request.getInitiatorId();
      token = null;
    }
    else {
      userId = null;
      token = request.getInitiatorId();
    }

    return new org.sonatype.nexus.common.app.FreezeRequest(token, "", request.getTimestamp(), userId, null);
  }
}
