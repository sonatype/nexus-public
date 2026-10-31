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
package org.sonatype.nexus.repository.rest.api;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.COMPONENT;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.PROPRIETARY_COMPONENTS;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.WRITE_POLICY;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_ATTRIBUTES_KEY;
import static org.sonatype.nexus.repository.manager.internal.BaseRepositoryManager.CLEANUP_NAME_KEY;

/**
 * @since 3.20
 */
@Component
public class HostedRepositoryApiRequestToConfigurationConverter<S extends HostedStorageAttributes, T extends HostedRepositoryApiRequest<S>>
    extends AbstractRepositoryApiRequestToConfigurationConverter<S, T>
{
  @Override
  public Configuration convert(final T request) {
    Configuration configuration = super.convert(request);

    if (request.getComponent() != null) {
      configuration.attributes(COMPONENT)
          .set(PROPRIETARY_COMPONENTS, request.getComponent().getProprietaryComponents());
    }

    maybeAddDataStoreName(configuration);

    if (request.getCleanup() != null) {
      configuration.attributes(CLEANUP_ATTRIBUTES_KEY)
          .set(CLEANUP_NAME_KEY, Sets.newHashSet(request.getCleanup().getPolicyNames()));
    }
    return configuration;
  }

  @Override
  protected void convertStorage(final T request, final Configuration configuration) {
    super.convertStorage(request, configuration);
    StorageAttributes storage = request.getStorage();
    if (nonNull(storage)) {
      configuration.attributes(STORAGE).set(WRITE_POLICY, request.getStorage().getWritePolicy());
    }
  }
}
