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
package org.sonatype.nexus.plugins.capabilities.internal.storage;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.kazuki.v0.internal.v2schema.Attribute;
import io.kazuki.v0.internal.v2schema.Attribute.Type;
import io.kazuki.v0.internal.v2schema.Schema;
import io.kazuki.v0.store.KazukiException;
import io.kazuki.v0.store.Key;
import io.kazuki.v0.store.keyvalue.KeyValueIterable;
import io.kazuki.v0.store.keyvalue.KeyValuePair;
import io.kazuki.v0.store.keyvalue.KeyValueStore;
import io.kazuki.v0.store.lifecycle.Lifecycle;
import io.kazuki.v0.store.schema.SchemaStore;
import io.kazuki.v0.store.schema.TypeValidation;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Kauzki-based {@link CapabilityStorage}.
 *
 * @since 2.8
 */
public class KazukiCapabilityStorage
    extends LifecycleSupport
    implements CapabilityStorage
{
  public static final String CAPABILITY_SCHEMA = "capability";

  private final ApplicationDirectories applicationDirectories;

  private final Lifecycle lifecycle;

  private final KeyValueStore keyValueStore;

  private final SchemaStore schemaStore;

  @Inject
  public KazukiCapabilityStorage(final ApplicationDirectories applicationDirectories,
                                 final @Named("nexuscapability") Lifecycle lifecycle,
                                 final @Named("nexuscapability") KeyValueStore keyValueStore,
                                 final @Named("nexuscapability") SchemaStore schemaStore)
  {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.lifecycle = checkNotNull(lifecycle);
    this.keyValueStore = checkNotNull(keyValueStore);
    this.schemaStore = checkNotNull(schemaStore);
  }

  /**
   * Helper to determine if KZ-based storage database exists.
   *
   * @since 2.9
   */
  public boolean exists() {
    File dir = applicationDirectories.getWorkDirectory("db/capabilities", false);
    File file = new File(dir, "capabilities.h2.db");
    return file.exists();
  }

  /**
   * Helper to drop entire KZ-based storage database.
   *
   * @since 2.9
   */
  public void drop() {
    File dir = applicationDirectories.getWorkDirectory("db/capabilities", false);
    try {
      DirSupport.deleteIfExists(dir.toPath());
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected void doStart() throws Exception {
    lifecycle.init();
    lifecycle.start();

    if (schemaStore.retrieveSchema(CAPABILITY_SCHEMA) == null) {
      Schema schema = new Schema(ImmutableList.of(
          new Attribute("version", Type.I32, null, false),
          new Attribute("type", Type.UTF8_SMALLSTRING, null, true),
          new Attribute("enabled", Type.BOOLEAN, null, true),
          new Attribute("notes", Type.UTF8_TEXT, null, true),
          new Attribute("properties", Type.MAP, null, true)));

      log.info("Creating schema for 'capability' type");

      schemaStore.createSchema(CAPABILITY_SCHEMA, schema);
    }
  }

  @Override
  protected void doStop() throws Exception {
    lifecycle.stop();
    lifecycle.shutdown();
  }

  @Override
  public CapabilityIdentity add(final CapabilityStorageItem item) throws IOException {
    try {
      return asCapabilityIdentity(
          keyValueStore.create(CAPABILITY_SCHEMA, CapabilityStorageItem.class, item, TypeValidation.STRICT)
      );
    }
    catch (KazukiException e) {
      throw new IOException("Capability could not be added", e);
    }
  }

  @Override
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) throws IOException {
    try {
      return keyValueStore.update(asKey(id), CapabilityStorageItem.class, item);
    }
    catch (KazukiException e) {
      throw new IOException("Capability could not be updated", e);
    }
  }

  @Override
  public boolean remove(final CapabilityIdentity id) throws IOException {
    try {
      return keyValueStore.delete(asKey(id));
    }
    catch (KazukiException e) {
      throw new IOException("Capability could not be removed", e);
    }
  }

  @Override
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() throws IOException {
    Map<CapabilityIdentity, CapabilityStorageItem> items = Maps.newHashMap();

    try (KeyValueIterable<KeyValuePair<CapabilityStorageItem>> entries = keyValueStore.iterators().entries(
        CAPABILITY_SCHEMA, CapabilityStorageItem.class
    )) {
      for (KeyValuePair<CapabilityStorageItem> entry : entries) {
        items.put(asCapabilityIdentity(entry.getKey()), entry.getValue());
      }
    }

    return items;
  }

  private Key asKey(final CapabilityIdentity id) {
    return keyValueStore.toKey("@" + CAPABILITY_SCHEMA + ":" + id.toString());
  }

  private CapabilityIdentity asCapabilityIdentity(final Key key) {
    return new CapabilityIdentity(key.getIdPart());
  }

}
