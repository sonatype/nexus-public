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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventConsumer;
import org.sonatype.nexus.repository.config.ConfigurationCreatedEvent;
import org.sonatype.nexus.repository.config.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.ConfigurationEvent;
import org.sonatype.nexus.repository.config.ConfigurationUpdatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * Repository configuration subscriber on DB events like CREATE/UPDATE/DELETE.
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class ConfigurationSubscriber
    extends ComponentSupport
    implements EventAware
{
  private final RepositoryManager repositoryManager;

  @Inject
  public ConfigurationSubscriber(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Subscribe
  public void on(final ConfigurationCreatedEvent event) {
    handleReplication(event, e ->
        repositoryManager.create(repositoryManager.retrieveConfigurationByName(e.getRepositoryName())
            .orElseThrow(() -> new RuntimeException("Missing configuration: " + e.getRepositoryName()))));
  }

  @Subscribe
  public void on(final ConfigurationUpdatedEvent event) {
    handleReplication(event, e ->
        repositoryManager.update(repositoryManager.retrieveConfigurationByName(e.getRepositoryName())
            .orElseThrow(() -> new RuntimeException("Missing configuration: " + e.getRepositoryName()))));
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
      catch (Exception e) {
        log.error("Failed to replicate: {}", event, e);
      }
    }
  }
}
