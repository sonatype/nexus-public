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

import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;

import com.google.common.annotations.VisibleForTesting;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link DatabaseFreezeService}
 *
 * @since 3.2
 */
@Named
@Singleton
public class DatabaseFreezeServiceImpl
    extends ComponentSupport
    implements DatabaseFreezeService, EventAware
{
  private final Set<Provider<DatabaseInstance>> providers;

  private final EventBus eventBus;

  @VisibleForTesting
  volatile boolean frozen = false;

  @Inject
  public DatabaseFreezeServiceImpl(final Set<Provider<DatabaseInstance>> providers, final EventBus eventBus) {
    this.providers = checkNotNull(providers);
    this.eventBus = checkNotNull(eventBus);
  }

  @Override
  public synchronized void freezeAllDatabases() {
    if (frozen) {
      log.info("Databases already frozen, skipping freeze command.");
      return;
    }
    log.info("Freezing all databases.");
    frozen = true;

    processAll(db -> db.freeze(true));

    eventBus.post(new DatabaseFreezeChangeEvent(true));
  }

  @Override
  public synchronized void releaseAllDatabases() {
    if (!frozen) {
      log.info("Databases already released, skipping release command.");
      return;
    }

    log.info("Releasing all databases.");
    frozen = false;

    processAll(ODatabaseDocumentTx::release);

    eventBus.post(new DatabaseFreezeChangeEvent(false));
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  private void processAll(final Consumer<ODatabaseDocumentTx> databaseDocumentTxConsumer) {
    providers.forEach(provider -> {
      DatabaseInstance databaseInstance = provider.get();
      try (ODatabaseDocumentTx db = databaseInstance.connect()) {
        databaseDocumentTxConsumer.accept(db);
      }
      catch (Exception e) {
        log.warn("Unable to process Database instance: {}", databaseInstance, e);
      }
    });
  }
}
