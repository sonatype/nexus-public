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
package org.sonatype.nexus.rapture.internal.state;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Objects;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * State Ext.Direct component.
 *
 * @since 3.0
 */
@Component
@DirectAction(action = "rapture_State")
public class StateComponent
    extends DirectComponentSupport
{
  /**
   * Randomly generated identifier on each boot to identify the running instance of the server and detect server
   * reboots.
   */
  private final String serverId;

  private final List<StateContributor> stateContributors;

  @Autowired
  public StateComponent(@Lazy final List<StateContributor> stateContributors, final NodeAccess nodeAccess) {
    this.stateContributors = checkNotNull(stateContributors);
    // special key on serverId hints UI event listeners to ignore serverId changes
    final String prefix = checkNotNull(nodeAccess).isClustered() ? "ignore." : "";
    this.serverId = prefix + String.valueOf(System.nanoTime());
  }

  @Timed
  @ExceptionMetered
  @DirectPollMethod(event = "rapture_State_get")
  public Map<String, Object> getState(final Map<String, String> hashes) {
    HashMap<String, Object> values = new HashMap<>();

    // First add an entry for each hash we got.
    // If state will not contribute a value for it, the state will be send back as null and such will be removed in UI
    for (String key : hashes.keySet()) {
      values.put(key, null);
    }

    for (StateContributor contributor : stateContributors) {
      try {
        Map<String, Object> stateValues = contributor.getState();
        if (stateValues != null) {
          for (Entry<String, Object> entry : stateValues.entrySet()) {
            if (!Strings2.isBlank(entry.getKey())) {
              maybeSend(values, hashes, entry.getKey(), entry.getValue());
            }
            else {
              log.warn("Blank state-id returned by {} (ignored)", contributor.getClass().getName());
            }
          }
        }
      }
      catch (Exception e) {
        log.warn("Failed to get state from {} (ignored)", contributor.getClass().getName(), e);
      }
    }

    maybeSend(values, hashes, "serverId", serverId);

    return values;
  }

  /**
   * Include value in state unless its hash is unchanged.
   */
  private void maybeSend(
      final Map<String, Object> values,
      final Map<String, String> hashes,
      final String key,
      final Object value)
  {
    values.remove(key);
    String hash = hash(value);
    if (!Objects.equal(hash, hashes.get(key))) {
      StateValueXO data = new StateValueXO();
      data.setHash(hash);
      data.setValue(value);
      values.put(key, data);
    }
  }

  /**
   * Gson instance used to calculate hashes.
   */
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create(); // pretty for debugging

  /**
   * Calculate (opaque) hash for given non-null value.
   */
  @Nullable
  private String hash(@Nullable final Object value) {
    if (value != null) {
      // TODO: consider using Object.hashCode() and getting state contributors to ensure values have proper impls?
      // TODO: ... or something else which is more efficient than object->gson->sha1?
      String json = gson.toJson(value);
      log.trace("Hashing state: {}", json);
      return Hashing.sha1().hashString(json, StandardCharsets.UTF_8).toString();
    }
    return null;
  }

}
