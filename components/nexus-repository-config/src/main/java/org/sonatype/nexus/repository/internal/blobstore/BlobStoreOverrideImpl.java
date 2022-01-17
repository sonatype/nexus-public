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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * A {@code BlobStoreOverride} that allows blob store attributes to be overridden
 * via an environment variable ({@code NEXUS_BLOB_STORE_OVERRIDE}) during {@code BlobStoreManager}
 * initialization. e.g.:
 * <pre>
 *NEXUS_BLOB_STORE_OVERRIDE='{"default":{"file":{"path":"other_path"}}}'
 * </pre>
 *
 * @since 3.31
 */
@FeatureFlag(name = "nexus.blobstore.override.enabled", enabledByDefault = true)
@Named
@Singleton
public class BlobStoreOverrideImpl
    extends ComponentSupport
    implements BlobStoreOverride
{
  static final String NEXUS_BLOB_STORE_OVERRIDE = "NEXUS_BLOB_STORE_OVERRIDE";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final TypeReference<Map<String, Map<String, Map<String, Object>>>> TYPE_REFERENCE =
      new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {};

  private final BlobStoreConfigurationStore blobStoreConfigurationStore;

  @Inject
  public BlobStoreOverrideImpl(final BlobStoreConfigurationStore blobStoreConfigurationStore) {
    this.blobStoreConfigurationStore = checkNotNull(blobStoreConfigurationStore);
  }

  @Override
  public void apply() {
    String overrideJson = System.getenv(NEXUS_BLOB_STORE_OVERRIDE);

    if (isBlank(overrideJson)) {
      return;
    }

    try {
      Map<String, Map<String, Map<String, Object>>> overrides = OBJECT_MAPPER.readValue(overrideJson, TYPE_REFERENCE);
      if (overrides == null) {
        log.warn("{} parsed to null: {}", NEXUS_BLOB_STORE_OVERRIDE, overrideJson);
        return;
      }
      List<BlobStoreConfiguration> blobStoreConfigs = blobStoreConfigurationStore.list();
      log.debug("Applying blob store overrides: {}", overrides);
      blobStoreConfigs.stream()
          .filter(config -> overrides.containsKey(config.getName()))
          .forEach(config -> {
            Map<String, Map<String, Object>> changes = overrides.get(config.getName());
            if (changes != null && !changes.isEmpty()) {
              log.debug("Merging changes into blob store {}: {}", config.getName(), changes);
              if (mergeChangesToMap(changes, config.getAttributes())) {
                blobStoreConfigurationStore.update(config);
              }
            }
          });
    }
    catch (JsonProcessingException e) {
      log.error("Unable to parse {}: {}", NEXUS_BLOB_STORE_OVERRIDE, overrideJson);
      throw new IllegalStateException("Unable to parse environment variable NEXUS_BLOB_STORE_OVERRIDE", e);
    }
  }

  private boolean mergeChangesToMap(Map<String, Map<String, Object>> changes, Map<String, Map<String, Object>> map) {
    boolean changed = false;
    for (Entry<String, Map<String, Object>> l1 : changes.entrySet()) {
      if (l1.getValue() != null) {
        for (Entry<String, Object> l2 : l1.getValue().entrySet()) {
          Map<String, Object> l1Map = map.get(l1.getKey());
          if (l1Map != null && !l2.getValue().equals(l1Map.get(l2.getKey()))) {
            l1Map.put(l2.getKey(), l2.getValue());
            changed = true;
          }
        }
      }
    }
    return changed;
  }
}
