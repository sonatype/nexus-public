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
package org.sonatype.nexus.internal.app;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.app.Freezable;
import org.sonatype.nexus.common.app.FreezeRequest;
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.FrozenException;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.freeze.event.FreezeForceReleaseEvent;
import org.sonatype.nexus.freeze.event.FreezeReleaseEvent;
import org.sonatype.nexus.freeze.event.FreezeRequestEvent;
import org.sonatype.nexus.security.ClientInfo;
import org.sonatype.nexus.security.ClientInfoProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Lists.reverse;
import static java.util.Optional.ofNullable;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTime.parse;
import static org.joda.time.DateTimeZone.UTC;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * Local {@link FreezeService} that remembers user-initiated freeze requests between restarts.
 *
 * @since 3.24
 */
@Named
@ManagedLifecycle(phase = STORAGE)
@Singleton
public class LocalFreezeService
    extends StateGuardLifecycleSupport
    implements FreezeService
{
  private static final String MARKER_FILE = "frozen.marker";

  private final ObjectMapper mapper = new ObjectMapper();

  private final List<FreezeRequest> freezeRequests = new CopyOnWriteArrayList<>();

  private final File markerFile;

  private final ClientInfoProvider clientInfoProvider;

  private final List<Freezable> freezables;

  private final EventManager eventManager;

  @Inject
  public LocalFreezeService(final ApplicationDirectories directories,
                            final ClientInfoProvider clientInfoProvider,
                            final List<Freezable> freezables,
                            final EventManager eventManager)
  {
    this.markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
    this.freezables = checkNotNull(freezables);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  protected void doStart() {
    synchronized (freezeRequests) {
      if (loadUserFreezeRequest()) {
        freezables.forEach(this::tryFreeze);
      }
    }
  }

  @Override
  public void requestFreeze(final String reason) {
    addRequest(null, reason);
  }

  @Override
  public void cancelFreeze() {
    removeRequest(null);
  }

  @Override
  public void taskRequestFreeze(final String token, final String reason) {
    addRequest(checkNotNull(token), reason);
  }

  @Override
  public void taskCancelFreeze(final String token) {
    removeRequest(checkNotNull(token));
  }

  @Override
  public List<FreezeRequest> cancelAllFreezeRequests() {
    eventManager.post(new FreezeForceReleaseEvent());
    List<FreezeRequest> canceledRequests = currentFreezeRequests();
    freezeRequests.clear();
    reverse(freezables).forEach(this::tryUnfreeze);
    return canceledRequests;
  }

  @Override
  public boolean isFrozen() {
    return !freezeRequests.isEmpty();
  }

  @Override
  public List<FreezeRequest> currentFreezeRequests() {
    return ImmutableList.copyOf(freezeRequests);
  }

  @Override
  public void checkReadable(final String errorMessage) {
    // not implemented by the in-memory service
  }

  @Override
  public void checkWritable(final String errorMessage) {
    if (isFrozen()) {
      throw new FrozenException(errorMessage);
    }
  }

  private void addRequest(@Nullable final String token, final String reason) {
    FreezeRequest request = newRequest(token, reason);
    eventManager.post(new FreezeRequestEvent(reason));
    synchronized (freezeRequests) {
      checkState(!any(freezeRequests, sameToken(token)), "Freeze has already been requested");
      if (token == null) {
        saveUserFreezeRequest(request);
      }
      freezeRequests.add(request);
      if (freezeRequests.size() == 1) {
        freezables.forEach(this::tryFreeze);
      }
    }
  }

  private void removeRequest(@Nullable final String token) {
    eventManager.post(new FreezeReleaseEvent());
    if (token == null) {
      deleteUserFreezeRequest();
    }
    synchronized (freezeRequests) {
      checkState(freezeRequests.removeIf(sameToken(token)), "Cannot find freeze request to cancel");
      if (freezeRequests.size() == 0) {
        reverse(freezables).forEach(this::tryUnfreeze);
      }
    }
  }

  private FreezeRequest newRequest(@Nullable final String token, final String reason) {
    Optional<ClientInfo> clientInfo = ofNullable(clientInfoProvider.getCurrentThreadClientInfo());
    return new FreezeRequest(token, reason, now(UTC),
        clientInfo.map(ClientInfo::getUserid).orElse(null),
        clientInfo.map(ClientInfo::getRemoteIP).orElse(null));
  }

  private static Predicate<FreezeRequest> sameToken(@Nullable final String token) {
    Optional<String> optionalToken = ofNullable(token);
    return request -> optionalToken.equals(request.token());
  }

  private void tryFreeze(final Freezable freezable) {
    try {
      freezable.freeze();
    }
    catch (Exception e) {
      log.warn("Problem freezing {}", freezable, e);
    }
  }

  private void tryUnfreeze(final Freezable freezable) {
    try {
      freezable.unfreeze();
    }
    catch (Exception e) {
      log.warn("Problem unfreezing {}", freezable, e);
    }
  }

  /**
   * @return {@code true} if a previously saved request exists and is the first to be added
   */
  private boolean loadUserFreezeRequest() {
    if (markerFile.exists()) {
      FreezeRequest request;
      try {
        JsonNode json = mapper.readTree(markerFile);
        if (json.size() == 0) {
          return false;
        }

        request = new FreezeRequest(null,
            json.path("reason").asText(),
            parse(json.path("frozenAt").asText()),
            json.path("frozenBy").asText(),
            json.path("frozenByIp").asText());
      }
      catch (Exception e) {
        log.debug("Problem parsing " + MARKER_FILE + ", will add placeholder request", e);
        request = new FreezeRequest(null, MARKER_FILE, now(UTC), null, null);
      }

      freezeRequests.add(request);
      return freezeRequests.size() == 1;
    }

    return false;
  }

  private void saveUserFreezeRequest(final FreezeRequest request) {
    try {
      Map<String, Object> json = ImmutableMap.of(
          "reason", request.reason(),
          "frozenAt", request.frozenAt().toString(),
          "frozenBy", request.frozenBy().orElse(null),
          "frozenByIp", request.frozenByIp().orElse(null));

      mapper.writeValue(markerFile, json);
    }
    catch (Exception e) {
      log.warn("Cannot save " + MARKER_FILE, e);
    }
  }

  private void deleteUserFreezeRequest() {
    if (markerFile.exists()) {
      try {
        Files.delete(markerFile.toPath());
      }
      catch (Exception e) {
        log.warn("Cannot delete " + MARKER_FILE, e);
      }
    }
  }
}
