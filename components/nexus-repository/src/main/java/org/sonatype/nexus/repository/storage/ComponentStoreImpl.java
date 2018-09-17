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
package org.sonatype.nexus.repository.storage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Repository;

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.SCHEMAS;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.GROUP;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.NAME;

/**
 * @since 3.6
 */
@Singleton
@ManagedLifecycle(phase = SCHEMAS)
@Named
public class ComponentStoreImpl
    extends StateGuardLifecycleSupport
    implements ComponentStore, Lifecycle
{
  private final Provider<DatabaseInstance> databaseInstance;

  private final ComponentEntityAdapter entityAdapter;

  @Inject
  public ComponentStoreImpl(@Named("component") final Provider<DatabaseInstance> databaseInstance,
                            final ComponentEntityAdapter entityAdapter)
  {
    this.databaseInstance = checkNotNull(databaseInstance);
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Override
  @Guarded(by = STARTED)
  public Component read(final EntityId id)
  {
    try (ODatabaseDocumentTx db = databaseInstance.get().acquire()) {
      return entityAdapter.read(db, id);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public List<Component> getAllMatchingComponents(final Repository repository,
                                                  final String group,
                                                  final String name,
                                                  final Map<String, String> formatAttributes)
  {
    checkNotNull(repository);
    checkNotNull(group);
    checkNotNull(name);
    checkNotNull(formatAttributes);
    List<Component> filteredComponents;
    try (StorageTx storageTx = repository.facet(StorageFacet.class).txSupplier().get()) {
      storageTx.begin();

      Query.Builder query = Query.builder()
          .where(GROUP).eq(group)
          .and(NAME).eq(name)
          .suffix("order by version desc");

      Iterable<Component> unfilteredComponents = storageTx.findComponents(query.build(), singletonList(repository));
      Stream<Component> filteredStream = StreamSupport.stream(unfilteredComponents.spliterator(), false)
          .filter(
              (component) -> formatAttributes.entrySet().stream().allMatch(
                  (entry) -> Objects.equals(entry.getValue(), component.formatAttributes().get(entry.getKey()))
              )
          );

      // Copy objects into a list so that the references aren't cleared after storageTx is closed - See NEXUS-17927
      filteredComponents = ImmutableList.copyOf(filteredStream.iterator());
    }

    return filteredComponents;
  }
}
