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
package org.sonatype.nexus.proxy.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.StringJoiner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemStatus;
import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.events.NexusStartedEvent;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.AbstractRepository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.Subscribe;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.write;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonatype.nexus.proxy.storage.StorageWhitelist.PROP_NAME;

/**
 * On upgrade this will populate the {@code nexus.override.local.storage.whitelist} property in {@code nexus.properties}
 * if it is not found. Its value will be created from all existing repositories which have a value set for
 * {@code overrideLocalStorageUrl}
 *
 * @since 2.14.15
 */
@Singleton
@Named("StorageWhitelistUpgradeEvent")
public class StorageWhitelistUpgradeEvent
    extends ComponentSupport
    implements EventSubscriber
{
  private final ApplicationStatusSource applicationStatusSource;

  private final ApplicationDirectories applicationDirectories;

  private final RepositoryRegistry repositoryRegistry;

  private final StorageWhitelist storageWhitelist;

  @Inject
  public StorageWhitelistUpgradeEvent(final ApplicationStatusSource applicationStatusSource,
                                      final ApplicationDirectories applicationDirectories,
                                      final RepositoryRegistry repositoryRegistry,
                                      final StorageWhitelist storageWhitelist)
  {
    this.applicationStatusSource = requireNonNull(applicationStatusSource);
    this.applicationDirectories = requireNonNull(applicationDirectories);
    this.repositoryRegistry = requireNonNull(repositoryRegistry);
    this.storageWhitelist = requireNonNull(storageWhitelist);
  }

  @Subscribe
  public void inspect(final NexusStartedEvent startedEvent) {
    final SystemStatus systemStatus = applicationStatusSource.getSystemStatus();

    if (systemStatus.isInstanceUpgraded()) {
      try {
        if (notExists(nexusPropertiesPath())) {
          createFile(nexusPropertiesPath());
        }

        try (InputStream nexusPropsIn = newInputStream(nexusPropertiesPath(), READ)) {
          Properties nexusProps = new Properties();
          nexusProps.load(nexusPropsIn);

          if (!nexusProps.containsKey(PROP_NAME)) {
            log.info("System upgrade detected: creating repo storage whitelist property: '{}'", PROP_NAME);

            StringJoiner whitelistValue = new StringJoiner(",");
            repositoryRegistry.getRepositories().forEach(repo -> {
              String overrideLocalStorageUrl =
                  ((AbstractRepository) repo).getCurrentCoreConfiguration().getConfiguration(false)
                      .getLocalStorage().getUrl();

              if (isNotBlank(overrideLocalStorageUrl)) {
                log.info("Adding existing configured location '{}' to whitelisted storage locations",
                    overrideLocalStorageUrl);
                whitelistValue.add(overrideLocalStorageUrl);
                storageWhitelist.addWhitelistPath(overrideLocalStorageUrl);
              }
            });

            write(nexusPropertiesPath(), createSection(whitelistValue.toString()).getBytes(UTF_8), CREATE, APPEND);
            log.info("nexus.properties updated with '{}' property", PROP_NAME);
          }
        }
      }
      catch (IOException e) {
        log.error("Unable to upgrade nexus.properties with whitelisted storage", e);
      }
    }
  }

  private Path nexusPropertiesPath() {
    return Paths.get(applicationDirectories.getInstallDirectory().getAbsolutePath(), "/conf/nexus.properties");
  }

  private String createSection(final String propValues) {
    StringBuilder propBuilder = new StringBuilder();
    propBuilder.append(lineSeparator());
    propBuilder.append("# Storage whitelist");
    propBuilder.append(lineSeparator());
    propBuilder.append("# Generated using existing configured values from all repositories if not found on upgrade");
    propBuilder.append(lineSeparator());
    propBuilder.append("# Comma-separated list of allowed override storage locations for repositories");
    propBuilder.append(lineSeparator());
    propBuilder.append("# See https://links.sonatype.com/products/nxrm2/configuring-repositories");
    propBuilder.append(lineSeparator());
    propBuilder.append(PROP_NAME).append("=").append(propValues);

    return propBuilder.toString();
  }
}
