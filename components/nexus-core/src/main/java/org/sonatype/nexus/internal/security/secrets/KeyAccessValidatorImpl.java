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
package org.sonatype.nexus.internal.security.secrets;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.time.Clock;
import org.sonatype.nexus.crypto.secrets.KeyAccessValidator;
import org.sonatype.nexus.crypto.secrets.EncryptionKeyValidator;
import org.sonatype.nexus.crypto.secrets.MissingKeyException;
import org.sonatype.nexus.crypto.secrets.ReportKnownSecretKeyEvent;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.node.datastore.NodeHeartbeat;
import org.sonatype.nexus.node.datastore.NodeHeartbeatManager;
import org.sonatype.nexus.scheduling.PeriodicJobService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.node.datastore.NodeHeartbeatManager.NODE_ID;

/**
 * Implementation of {@link KeyAccessValidator}. It sends an event to all nodes, waits for all nodes to respond and then
 * checks if all nodes have access to the key.
 */
@Named
@Singleton
public class KeyAccessValidatorImpl
    extends StateGuardLifecycleSupport
    implements KeyAccessValidator, EventAware
{
  private static final long SECOND_IN_MILLISECONDS = 1000;

  private static final String KEY_ID_ACCESS_FORMAT = "re-encrypt.key.access.%s";

  private final EncryptionKeyValidator encryptionKeyValidator;

  private final NodeHeartbeatManager nodeHeartbeatManager;

  private final PeriodicJobService periodicJobService;

  private final EventManager eventManager;

  private final ObjectMapper objectMapper;

  private final NodeAccess nodeAccess;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final Clock clock;

  private final int timeoutSeconds;

  @Inject
  public KeyAccessValidatorImpl(
      final EncryptionKeyValidator encryptionKeyValidator,
      @Nullable final NodeHeartbeatManager nodeHeartbeatManager,
      final PeriodicJobService periodicJobService,
      final EventManager eventManager,
      final ObjectMapper objectMapper,
      final NodeAccess nodeAccess,
      final GlobalKeyValueStore globalKeyValueStore,
      final Clock clock,
      @Named("${nexus.distributed.events.fetch.interval.seconds:-5}") final int pollIntervalSeconds)
  {
    this.encryptionKeyValidator = checkNotNull(encryptionKeyValidator);
    this.nodeHeartbeatManager = nodeHeartbeatManager;
    this.periodicJobService = checkNotNull(periodicJobService);
    this.eventManager = checkNotNull(eventManager);
    this.objectMapper = checkNotNull(objectMapper);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
    this.clock = checkNotNull(clock);
    this.timeoutSeconds = pollIntervalSeconds * 2;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ReportKnownSecretKeyEvent event) {
    log.debug("Received ReportKnownSecretKeyEvent");
    String nodeKey = getNodeKey(nodeAccess.getId());
    globalKeyValueStore.setKey(
        new NexusKeyValue(nodeKey, ValueType.OBJECT, buildNodeKeyAccessMap(event.getKeyId())));
    periodicJobService.runOnce(() -> globalKeyValueStore.removeKey(nodeKey), timeoutSeconds);
  }

  @Override
  public boolean isValidKey(final String keyId) {
    OffsetDateTime initiatedAt = clock.clusterTime();
    eventManager.post(new ReportKnownSecretKeyEvent(keyId));
    return isKeyOnAllNodes(initiatedAt);
  }

  private boolean isKeyOnAllNodes(final OffsetDateTime initiatedAt) {
    long startTime = System.currentTimeMillis();
    long timeOutInMs = timeoutSeconds * SECOND_IN_MILLISECONDS;

    Set<String> activeNodeIds = getActiveNodeIds();
    Set<String> withAccess = new HashSet<>();

    try {
      while (true) {
        activeNodeIds.stream()
            .filter(nodeId -> !withAccess.contains(nodeId))
            .filter(nodeId -> hasAccess(nodeId, initiatedAt))
            .forEach(withAccess::add);

        if (activeNodeIds.size() == withAccess.size()) {
          return true;
        }

        if (System.currentTimeMillis() - startTime > timeOutInMs) {
          throw new TimeoutException();
        }
        Thread.sleep(100); // wait for nodes to respond with key access info
      }
    }
    catch (TimeoutException timeout) {
      log.debug("Timeout while waiting for all nodes to check key access");
    }
    catch (InterruptedException | MissingKeyException e) {
      // ignore
    }
    return false;
  }

  @VisibleForTesting
  protected Set<String> getActiveNodeIds() {
    Set<String> activeNodeIds = new HashSet<>();
    if (nodeAccess.isClustered()) {
      activeNodeIds = nodeHeartbeatManager
          .getActiveNodeHeartbeatData()
          .stream()
          .map(NodeHeartbeat::nodeInfo)
          .map(nodeInfo -> nodeInfo.get(NODE_ID))
          .filter(Objects::nonNull)
          .map(Object::toString)
          .collect(Collectors.toSet());
    }
    else {
      activeNodeIds.add(nodeAccess.getId());
    }
    return activeNodeIds;
  }

  private boolean hasAccess(final String nodeId, final OffsetDateTime initiatedAt) {
    Optional<Boolean> hasAccess = globalKeyValueStore.getKey(getNodeKey(nodeId))
        .map(val -> val.getAsObject(objectMapper, Map.class))
        .filter(val -> isKeyAccessDataAfterInitiatedAt((String) val.get("timestamp"), initiatedAt))
        .map(val -> (Boolean) val.get("hasAccess"));

    if (hasAccess.isPresent() && !hasAccess.get()) {
      log.debug("Node {} is missing access to the specified key", nodeId);
      throw new MissingKeyException("Missing key access on node: " + nodeId);
    }

    return hasAccess.orElse(false);
  }

  private boolean isKeyAccessDataAfterInitiatedAt(
      final String timestamp,
      final OffsetDateTime initiatedAt)
  {
    return OffsetDateTime.parse(timestamp).isAfter(initiatedAt);
  }

  private Map<String, Object> buildNodeKeyAccessMap(final String keyId) {
    Map<String, Object> nodeKeyAccessMap = new HashMap<>();
    nodeKeyAccessMap.put("keyId", keyId);
    nodeKeyAccessMap.put("hasAccess", hasKeyIdAccess(keyId));
    nodeKeyAccessMap.put("timestamp", clock.clusterTime().toString());
    return nodeKeyAccessMap;
  }

  private String getNodeKey(final String nodeId) {
    return String.format(KEY_ID_ACCESS_FORMAT, nodeId);
  }

  private boolean hasKeyIdAccess(final String keyId) {
    return encryptionKeyValidator.isValidKey(keyId);
  }
}
