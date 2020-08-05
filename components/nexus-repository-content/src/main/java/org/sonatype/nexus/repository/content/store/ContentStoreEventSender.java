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
package org.sonatype.nexus.repository.content.store;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Schedules sending of {@link ContentStoreEvent}s pre/post commit.
 *
 * Also makes sure each event has its {@link Repository} set if available.
 *
 * @since 3.26
 */
@Named
@Singleton
public class ContentStoreEventSender
    extends ComponentSupport
{
  private final ContentFacetFinder contentFacetFinder;

  private final EventManager eventManager;

  @Inject
  public ContentStoreEventSender(final ContentFacetFinder contentFacetFinder, final EventManager eventManager) {
    this.contentFacetFinder = checkNotNull(contentFacetFinder);
    this.eventManager = checkNotNull(eventManager);
  }

  public void preCommit(final Supplier<ContentStoreEvent> eventSupplier) {
    thisSession().preCommit(() -> sendContentStoreEvent(eventSupplier));
  }

  public void postCommit(final Supplier<ContentStoreEvent> eventSupplier) {
    thisSession().postCommit(() -> sendContentStoreEvent(eventSupplier));
  }

  private void sendContentStoreEvent(final Supplier<ContentStoreEvent> eventSupplier) {
    ContentStoreEvent event = eventSupplier.get();
    Optional<Repository> repository = contentFacetFinder.findRepository(event.contentRepositoryId);
    if (repository.isPresent()) {
      event.setRepository(repository.get());
      eventManager.post(event);
    }
    else {
      log.warn("Could not find repository for {}, ignoring event", event);
    }
  }

  private DataSession<?> thisSession() {
    return UnitOfWork.currentSession();
  }
}
