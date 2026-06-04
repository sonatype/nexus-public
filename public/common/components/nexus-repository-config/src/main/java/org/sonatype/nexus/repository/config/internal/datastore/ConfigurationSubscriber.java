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
package org.sonatype.nexus.repository.config.internal.datastore;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventConsumer;
import org.sonatype.nexus.repository.MissingRepositoryException;
import org.sonatype.nexus.repository.config.ConfigurationCreatedEvent;
import org.sonatype.nexus.repository.config.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.ConfigurationEvent;
import org.sonatype.nexus.repository.config.ConfigurationUpdatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Repository configuration subscriber on DB events like CREATE/UPDATE/DELETE.
 */
@Component
public class ConfigurationSubscriber
    implements EventAware
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final RepositoryManager repositoryManager;

  @Autowired
  public ConfigurationSubscriber(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Subscribe
  public void on(final ConfigurationCreatedEvent event) {
    handleReplication(event,
        e -> {
          // Handle missing configuration during event replication race conditions
          var config = repositoryManager.retrieveConfigurationByName(e.getRepositoryName());
          if (config.isEmpty()) {
            log.warn("Configuration '{}' not yet available during event replication, skipping create",
                e.getRepositoryName());
            return;
          }
          repositoryManager.create(config.get());
        });
  }

  @Subscribe
  public void on(final ConfigurationUpdatedEvent event) {
    handleReplication(event,
        e -> {
          // Handle missing configuration during event replication race conditions
          var config = repositoryManager.retrieveConfigurationByName(e.getRepositoryName());
          if (config.isEmpty()) {
            log.warn("Configuration '{}' not yet available during event replication, skipping update",
                e.getRepositoryName());
            return;
          }
          repositoryManager.update(config.get());
        });
  }

  @Subscribe
  public void on(final ConfigurationDeletedEvent event) {
    handleReplication(event, e -> repositoryManager.delete(e.getRepositoryName()));
  }

  private void handleReplication(final ConfigurationEvent event, final EventConsumer<ConfigurationEvent> consumer) {
    if (!event.isLocal()) {
      try {
        consumer.accept(event);
      }
      catch (MissingRepositoryException e) {
        log.debug("Race condition during replication of {}: {}", event, e.getMessage());
      }
      catch (Exception e) {
        log.error("Failed to replicate: {}", event, e);
      }
    }
  }
}
