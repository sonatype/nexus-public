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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.rapture.StateContributor;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectPollMethod;
import com.softwarementors.extjs.djn.servlet.ssm.WebContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * State Ext.Direct component.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = "rapture_State")
public class StateComponent
    extends DirectComponentSupport
{
  private static final Logger log = LoggerFactory.getLogger(StateComponent.class);

  private final List<Provider<StateContributor>> stateContributors;

  private final static String serverId = String.valueOf(System.nanoTime());

  @Inject
  public StateComponent(final List<Provider<StateContributor>> stateContributors) {
    this.stateContributors = checkNotNull(stateContributors);
  }

  @Timed
  @DirectPollMethod(event = "rapture_State_get")
  public Map<String, Object> getState(final Map<String, String> hashes) {
    HashMap<String, Object> values = new HashMap<>();

    // First add an entry for each hash we got.
    // If state will not contribute a value for it, the state will be send back as null and such will be removed in UI
    for (String key : hashes.keySet()) {
      values.put(key, null);
    }

    for (Provider<StateContributor> contributor : stateContributors) {
      try {
        Map<String, Object> stateValues = contributor.get().getState();
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
  private void maybeSend(final Map<String, Object> values,
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
  private static String hash(@Nullable final Object value) {
    if (value != null) {
      // TODO: consider using Object.hashCode() and getting state contributors to ensure values have proper impls?
      // TODO: ... or something else which is more efficient than object->gson->sha1?
      String json = gson.toJson(value);
      log.trace("Hashing state: {}", json);
      return Hashing.sha1().hashString(json, Charsets.UTF_8).toString();
    }
    return null;
  }

}
