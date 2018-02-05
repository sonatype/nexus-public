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
package org.sonatype.nexus.orient.freeze;

import java.util.List;

import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;

/**
 * Service that freezes and releases all OrientDB databases.
 *
 * @since 3.2
 */
public interface DatabaseFreezeService
{
  /**
   * Request that the databases be frozen. {@link InitiatorType#USER_INITIATED} is used for external actors, like
   * a nexus administrator using the UI feature, or invoking a REST request. {@link InitiatorType#SYSTEM} is
   * used by internal actors, like system tasks. {@link InitiatorType#SYSTEM} should not be released by external
   * actors.
   *
   * If the request is rejected due to existing requests, this method will return null.
   *
   * @param type the type of request, user or system initiated
   * @param initiatorId an identifier for the initiator of the request
   * @return the {@link FreezeRequest}, or null if the request was rejected
   */
  FreezeRequest requestFreeze(InitiatorType type, String initiatorId);

  /**
   * @return a never null, but potentially empty, {@link List} of open {@link FreezeRequest}s
   */
  List<FreezeRequest> getState();

  /**
   * Release an existing {@link FreezeRequest}, if present.
   * If this was the last open request, all databases will be "thawed", removing "read-only" status.
   * If thaw happens this method will emit an event.
   *
   * @param request the request to release
   * @return true if the request was released
   */
  boolean releaseRequest(FreezeRequest request);

  /**
   * Release an existing {@link org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType#USER_INITIATED}
   * {@link FreezeRequest}, if present.
   * If this was the last open request, all databases will be "thawed", removing "read-only" status.
   * If thaw happens this method will emit an event.
   *
   * @return true if a user initiated request was present and it was successfully released; false otherwise.
   */
  boolean releaseUserInitiatedIfPresent();

  /**
   * Release all existing {@link FreezeRequest}s.
   * The intent for this method is a immediate, cluster-wide thaw of all databases.
   * @return the list of {@link FreezeRequest}s that were released.
   */
  List<FreezeRequest> releaseAllRequests();

  /**
   * Immediately freezes all local OrientDB databases if not already frozen; a frozen database will not accept
   * writes.
   *
   * Callers should prefer {@link #requestFreeze(InitiatorType, String)} as it provides necessary safe guards between
   * system and user initiated requests as well as distributes the status if HA is enabled.
   */
  void freezeLocalDatabases();

  /**
   * Immediately thaws all local OrientDB if frozen; upon conclusion writes will be accepted.
   *
   * Callers should prefer {@link #releaseRequest(FreezeRequest)}.
   */
  void releaseLocalDatabases();

  /**
   * Returns whether local databases are currently frozen. May not reflect cluster state (e.g. {@link #getState()}).
   */
  boolean isFrozen();

  /**
   * More descriptive object reflecting {@link #isFrozen()}.
   *
   * @return the current {@link ReadOnlyState}; never null
   */
  ReadOnlyState getReadOnlyState();

  /**
   * Check {@link #isFrozen()} and throw a {@link OModificationOperationProhibitedException} if it is.
   *
   * @throws OModificationOperationProhibitedException thrown if database is frozen
   */
  void checkUnfrozen();

  /**
   * Check {@link #isFrozen()} and throw a {@link OModificationOperationProhibitedException} if it is.
   *
   * @param message Message used when constructing the  OModificationOperationProhibitedException
   * @throws OModificationOperationProhibitedException thrown if database is frozen
   */
  void checkUnfrozen(String message);
}
