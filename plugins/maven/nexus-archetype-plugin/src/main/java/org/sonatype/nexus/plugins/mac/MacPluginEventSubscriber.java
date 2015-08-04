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
package org.sonatype.nexus.plugins.mac;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RepositoryNotAvailableException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * EventSubscriber that listens to registry events, repo addition and removal, and simply "hooks" in the generated
 * Archetype catalog file to their root.
 *
 * @author cstamas
 * @since 2.7.0
 */
@Named
@Singleton
public class MacPluginEventSubscriber
    extends ComponentSupport
    implements EventSubscriber
{
  private static final String ARCHETYPE_PATH = "/archetype-catalog.xml";

  private final ContentClass maven2ContentClass;

  @Inject
  public MacPluginEventSubscriber(final @Named("maven2") ContentClass maven2ContentClass) {
    this.maven2ContentClass = checkNotNull(maven2ContentClass);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryRegistryEventAdd evt) {
    final Repository repository = evt.getRepository();
    // check do we need to handle it at all
    if (isArchetypeCatalogSubject(repository)) {
      manageArchetypeCatalog(repository);
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final RepositoryConfigurationUpdatedEvent evt) {
    final Repository repository = evt.getRepository();
    // check do we need to handle it at all
    if (isArchetypeCatalogSubject(repository)) {
      manageArchetypeCatalog(repository);
    }
  }

  /**
   * Archetype Catalog subjects are Maven2 hosted, proxy and group repositories that are In Service.
   */
  private boolean isArchetypeCatalogSubject(final Repository repository) {
    return maven2ContentClass.isCompatible(repository.getRepositoryContentClass())
        && repository.getLocalStatus().shouldServiceRequest()
        && (repository.getRepositoryKind().isFacetAvailable(HostedRepository.class)
        || repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class) ||
        repository.getRepositoryKind().isFacetAvailable(GroupRepository.class));
  }

  /**
   * Installs or uninstall the archetype catalog (content generated) file from supported repository.
   */
  private void manageArchetypeCatalog(final Repository repository) {
    if (repository.isIndexable()) {
      // "install" the archetype catalog
      try {
        final DefaultStorageFileItem file =
            new DefaultStorageFileItem(repository, new ResourceStoreRequest(ARCHETYPE_PATH), true, false,
                new StringContentLocator(ArchetypeContentGenerator.ID));
        file.setContentGeneratorId(ArchetypeContentGenerator.ID);
        repository.storeItem(false, file);
      }
      catch (RepositoryNotAvailableException e) {
        log.info("Unable to install the generated archetype catalog, repository {} is out of service",
            e.getRepository().getId());
      }
      catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.info("Unable to install the generated archetype catalog in repository {}:", repository, e);
        }
        else {
          log.info("Unable to install the generated archetype catalog in repository {}: {}/{}", repository,
              e.getClass(), e.getMessage());
        }
      }
    }
    else {
      // "uninstall" the archetype catalog
      try {
        repository.deleteItem(false, new ResourceStoreRequest(ARCHETYPE_PATH));
      }
      catch (RepositoryNotAvailableException e) {
        log.info("Unable to uninstall the generated archetype catalog, repository {} is out of service",
            e.getRepository().getId());
      }
      catch (ItemNotFoundException e) {
        // neglect this, it was not present
      }
      catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.info("Unable to uninstall the generated archetype catalog in repository {}:", repository, e);
        }
        else {
          log.info("Unable to uninstall the generated archetype catalog in repository {}: {}/{}", repository,
              e.getClass(), e.getMessage());
        }
      }
    }
  }
}
