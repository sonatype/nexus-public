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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.rest.api.model.AbstractRepositoryApiRequest;

import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * @since 3.20
 */
public abstract class AbstractRepositoryApiRequestToConfigurationConverter<T extends AbstractRepositoryApiRequest>
{
  @Inject
  protected ConfigurationStore configurationStore;

  @Named(ORIENT_ENABLED)
  @Inject
  protected boolean orientEnabled;

  protected void maybeAddDataStoreName(final Configuration configuration) {
    if (!orientEnabled) {
      configuration.attributes(STORAGE).set(DATA_STORE_NAME, DEFAULT_DATASTORE_NAME);
    }
  }

  public Configuration convert(final T request) {
    Configuration configuration = configurationStore.newConfiguration();
    configuration.setRepositoryName(request.getName());
    configuration.setRecipeName(String.join("-", request.getFormat(), request.getType()));
    configuration.setOnline(request.getOnline());
    return configuration;
  }
}
