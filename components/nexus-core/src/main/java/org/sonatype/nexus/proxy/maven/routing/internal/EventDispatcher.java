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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.IOException;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDelete;
import org.sonatype.nexus.proxy.events.RepositoryItemEventDeleteRoot;
import org.sonatype.nexus.proxy.events.RepositoryItemEventStore;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal class routing various Nexus events to {@link Manager}.
 * <p>
 * Note: This component was initially marked as {@code @EagerSingleton}, but it did not play well with rest of plexus
 * components, as it broke everything. Seems like this component was created "too early", also, some UTs does not
 * prepare environment properly (like DefaultPasswordGeneratorTest, that does not set even the minimal properties
 * needed). Hence, this component is made a "plain" singleton (not eager), and {@link Manager} implementation will
 * pull it up, to have it created and to start ticking.
 *
 * @author cstamas
 * @since 2.4
 */
public class EventDispatcher
{
  private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

  private final Manager manager;

  /**
   * Da constructor.
   */
  public EventDispatcher(final Manager manager) {
    this.manager = checkNotNull(manager);
  }

  protected Logger getLogger() {
    return logger;
  }

  // actual work is done here

  protected void handleRepositoryAdded(final MavenRepository mavenRepository) {
    manager.initializePrefixFile(mavenRepository);
  }

  protected void handleRepositoryModified(final MavenRepository mavenRepository) {
    try {
      manager.forceUpdatePrefixFile(mavenRepository);
    }
    catch (IllegalStateException e) {
      // we will end up here regularly if reconfiguration was about putting repository out of service
      getLogger().debug("Repository {} is in bad state for prefix file update: {}",
          mavenRepository, e.getMessage());
    }
  }

  protected void handlePrefixFileUpdate(final RepositoryItemEvent evt) {
    final MavenRepository mavenRepository = (MavenRepository) evt.getRepository();
    try {
      final PrefixSource prefixSource = manager.getPrefixSourceFor(mavenRepository);
      manager.publish(mavenRepository, prefixSource);
    }
    catch (IOException e) {
      getLogger().warn("Problem while publishing prefix file for repository {}",
          RepositoryStringUtils.getHumanizedNameString(mavenRepository), e);
    }
  }

  protected void handlePrefixFileRemoval(final RepositoryItemEvent evt) {
    final MavenRepository mavenRepository = (MavenRepository) evt.getRepository();
    try {
      manager.unpublish(mavenRepository);
    }
    catch (IOException e) {
      getLogger().warn("Problem while unpublishing prefix file for repository {}",
          RepositoryStringUtils.getHumanizedNameString(mavenRepository), e);
    }
  }

  protected void offerPath(final MavenHostedRepository mavenHostedRepository, StorageItem item) {
    try {
      manager.offerEntry(mavenHostedRepository, item);
    }
    catch (IOException e) {
      getLogger().warn("Problem while maintaining prefix file for hosted repository {}, offered entry={}",
          RepositoryStringUtils.getHumanizedNameString(mavenHostedRepository), item, e);
    }
  }

  protected void revokePath(final MavenHostedRepository mavenHostedRepository, StorageItem item) {
    try {
      manager.revokeEntry(mavenHostedRepository, item);
    }
    catch (IOException e) {
      getLogger().warn("Problem while maintaining prefix file for hosted repository {}, revoked entry={}",
          RepositoryStringUtils.getHumanizedNameString(mavenHostedRepository), item, e);
    }
  }

  // == Filters

  protected boolean isRequestContextMarked(final RequestContext context) {
    return context.containsKey(Manager.ROUTING_INITIATED_FILE_OPERATION_FLAG_KEY);
  }

  protected boolean isRepositoryHandled(final Repository repository) {
    // we handle repository events if repo is not out of service, and only for non-shadow repository
    // that are Maven2 reposes
    return repository != null && repository.getRepositoryKind().isFacetAvailable(MavenRepository.class)
        && !repository.getRepositoryKind().isFacetAvailable(ShadowRepository.class)
        && Maven2ContentClass.ID.equals(repository.getRepositoryContentClass().getId());
  }

  protected boolean isPrefixFileEvent(final RepositoryItemEvent evt) {
    // is not fired as side effect of Publisher publishing this
    return isRepositoryHandled(evt.getRepository()) && !isRequestContextMarked(evt.getItem().getItemContext())
        && manager.isEventAboutPrefixFile(evt);
  }

  protected boolean isPlainItemEvent(final RepositoryItemEvent evt) {
    // is not fired as side effect of Publisher publishing this
    return isRepositoryHandled(evt.getRepository()) && !isRequestContextMarked(evt.getItem().getItemContext())
        && !evt.getItem().getRepositoryItemUid().getBooleanAttributeValue(IsHiddenAttribute.class);
  }

  protected boolean isPlainFileItemEvent(final RepositoryItemEvent evt) {
    // is not fired as side effect of Publisher publishing this
    return isPlainItemEvent(evt) && evt.getItem() instanceof StorageFileItem;
  }

  // == handlers for item events (to maintain prefix list file)

  /**
   * Event handler.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryItemEventStore(final RepositoryItemEventStore evt) {
    if (isPrefixFileEvent(evt)) {
      handlePrefixFileUpdate(evt);
    }
    else if (isPlainFileItemEvent(evt)) {
      // we maintain prefix list for hosted reposes only!
      final MavenHostedRepository mavenHostedRepository =
          evt.getRepository().adaptToFacet(MavenHostedRepository.class);
      if (mavenHostedRepository != null) {
        offerPath(mavenHostedRepository, evt.getItem());
      }
    }
  }

  /**
   * Event handler.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryItemEventCache(final RepositoryItemEventCache evt) {
    if (isPrefixFileEvent(evt)) {
      handlePrefixFileUpdate(evt);
    }
    else if (isPlainFileItemEvent(evt)) {
      // we maintain prefix list for hosted reposes only!
      final MavenHostedRepository mavenHostedRepository =
          evt.getRepository().adaptToFacet(MavenHostedRepository.class);
      if (mavenHostedRepository != null) {
        offerPath(mavenHostedRepository, evt.getItem());
      }
    }
  }

  /**
   * Event handler.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryItemEventDelete(final RepositoryItemEventDelete evt) {
    if (isPrefixFileEvent(evt)) {
      handlePrefixFileRemoval(evt);
    }
    else if (evt instanceof RepositoryItemEventDeleteRoot && isPlainItemEvent(evt)) {
      // we maintain prefix list for hosted reposes only!
      final MavenHostedRepository mavenHostedRepository =
          evt.getRepository().adaptToFacet(MavenHostedRepository.class);
      if (mavenHostedRepository != null) {
        revokePath(mavenHostedRepository, evt.getItem());
      }
    }
  }

  // == Handler for prefix list initialization

  /**
   * Event handler.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryRegistryEventAdd(final RepositoryRegistryEventAdd evt) {
    if (isRepositoryHandled(evt.getRepository())) {
      final MavenRepository mavenRepository = evt.getRepository().adaptToFacet(MavenRepository.class);
      handleRepositoryAdded(mavenRepository);
    }
  }

  // == Handlers for Proxy remote URL changes

  /**
   * Event handler.
   */
  @Subscribe
  @AllowConcurrentEvents
  public void onRepositoryConfigurationUpdatedEvent(final RepositoryConfigurationUpdatedEvent evt) {
    if (isRepositoryHandled(evt.getRepository())) {
      final MavenRepository mavenRepository = evt.getRepository().adaptToFacet(MavenRepository.class);
      handleRepositoryModified(mavenRepository);
    }
  }
}
