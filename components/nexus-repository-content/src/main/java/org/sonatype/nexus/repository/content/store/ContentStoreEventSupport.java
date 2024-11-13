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

import org.sonatype.nexus.common.event.Event;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.api.ContentDataAccess;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.SchemaTemplate;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetFinder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.common.text.Strings2.lower;

/**
 * Support class for format content stores that send events.
 *
 * @since 3.27
 */
public abstract class ContentStoreEventSupport<T extends ContentDataAccess>
    extends ContentStoreSupport<T>
{
  protected final String format;

  protected EventManager eventManager;

  private ContentFacetFinder contentFacetFinder;

  protected ContentStoreEventSupport(
      final DataSessionSupplier sessionSupplier,
      final String contentStoreName,
      final Class<T> daoClass)
  {
    super(sessionSupplier, contentStoreName, daoClass);
    this.format = extractFormat(daoClass);
  }

  @Inject
  protected void setDependencies(final ContentFacetFinder contentFacetFinder, final EventManager eventManager) {
    this.contentFacetFinder = checkNotNull(contentFacetFinder);
    this.eventManager = checkNotNull(eventManager);
  }

  public void preCommitEvent(final Supplier<Event> eventSupplier) {
    thisSession().preCommit(() -> postEvent(eventSupplier));
  }

  public void postCommitEvent(final Supplier<Event> eventSupplier) {
    thisSession().postCommit(() -> postEvent(eventSupplier));
  }

  private void postEvent(final Supplier<Event> eventSupplier) {
    Event event = eventSupplier.get();
    if (event instanceof ContentStoreEvent) {
      ContentStoreEvent cse = (ContentStoreEvent) event;
      cse.setRepositorySupplier(repositorySupplierFor(cse));
    }
    eventManager.post(event);
  }

  private Supplier<Optional<Repository>> repositorySupplierFor(final ContentStoreEvent event) {
    return () -> contentFacetFinder.findRepository(format, event.contentRepositoryId);
  }

  /**
   * Returns the lower-case form of the format after removing baseName from daoName.
   */
  private String extractFormat(final Class<T> daoClass) {
    // assume only one level between format and base DAO
    String formatDao = daoClass.getSimpleName();
    String dao = stream(daoClass.getInterfaces())
        .filter(c -> c.isAnnotationPresent(SchemaTemplate.class))
        .map(Class::getSimpleName)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Cannot determine format"));

    String prefix = lower(formatDao.substring(0, formatDao.length() - dao.length()));
    checkArgument(!prefix.isEmpty(), "%s must add a prefix to %s", formatDao, dao);
    return prefix;
  }
}
