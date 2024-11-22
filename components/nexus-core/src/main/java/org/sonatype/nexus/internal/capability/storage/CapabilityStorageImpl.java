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
package org.sonatype.nexus.internal.capability.storage;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemCreatedEventImpl;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemDeletedEventImpl;
import org.sonatype.nexus.internal.capability.storage.datastore.CapabilityStorageItemUpdatedEventImpl;
import org.sonatype.nexus.transaction.Transactional;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.UUID.fromString;
import static java.util.function.Function.identity;

/**
 * MyBatis {@link CapabilityStorage} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class CapabilityStorageImpl
    extends ConfigStoreSupport<CapabilityStorageItemDAO>
    implements CapabilityStorage
{
  @Inject
  public CapabilityStorageImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public CapabilityStorageItem newStorageItem(
      final int version,
      final String type,
      final boolean enabled,
      final String notes,
      final Map<String, String> properties)
  {
    CapabilityStorageItem item = new CapabilityStorageItemData();
    item.setVersion(version);
    item.setType(type);
    item.setEnabled(enabled);
    item.setNotes(notes);
    item.setProperties(properties);
    return item;
  }

  @Transactional
  @Override
  public CapabilityIdentity add(final CapabilityStorageItem item) {
    postCommitEvent(() -> new CapabilityStorageItemCreatedEventImpl((CapabilityStorageItemData) item));
    try {
      dao().create((CapabilityStorageItemData) item);
    }
    catch (DuplicateKeyException e) {
      log.debug("Trying to add duplicate for {} capability. Ignore it.", item);
    }
    return capabilityIdentity(item);
  }

  @Transactional
  @Override
  public boolean update(final CapabilityIdentity id, final CapabilityStorageItem item) {
    postCommitEvent(() -> new CapabilityStorageItemUpdatedEventImpl((CapabilityStorageItemData) item));
    ((HasEntityId) item).setId(entityId(id));
    return dao().update((CapabilityStorageItemData) item);
  }

  @Transactional
  @Override
  public boolean remove(final CapabilityIdentity id) {
    getAll().values()
        .stream()
        .filter(capability -> id.equals(capabilityIdentity(capability)))
        .findFirst()
        .map(CapabilityStorageItemData.class::cast)
        .ifPresent(item -> postCommitEvent(() -> new CapabilityStorageItemDeletedEventImpl(item)));
    return dao().delete(entityId(id));
  }

  @Transactional
  @Override
  public Map<CapabilityIdentity, CapabilityStorageItem> getAll() {
    return stream(dao().browse()).collect(toImmutableMap(CapabilityStorageImpl::capabilityIdentity, identity()));
  }

  @Transactional
  @Override
  public Map<CapabilityStorageItem, List<CapabilityIdentity>> browseCapabilityDuplicates() {
    return getAll().entrySet()
        .stream()
        .collect(Collectors.groupingBy(Entry::getValue))
        .entrySet()
        .stream()
        .filter(f -> f.getValue().size() > 1)
        .collect(Collectors.toMap(
            Entry::getKey,
            entry -> entry.getValue()
                .stream()
                .map(Entry::getKey)
                .collect(Collectors.toList())));
  }

  @Override
  public boolean isDuplicatesFound() {
    Map<CapabilityStorageItem, List<CapabilityIdentity>> duplicates = browseCapabilityDuplicates();
    log.debug("Found {} capability duplicates", duplicates.size());
    return !duplicates.isEmpty();
  }

  public static CapabilityIdentity capabilityIdentity(final CapabilityStorageItem item) {
    return new CapabilityIdentity(((HasEntityId) item).getId().getValue());
  }

  private static EntityId entityId(final CapabilityIdentity id) {
    return new EntityUUID(fromString(id.toString()));
  }
}
