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
package org.sonatype.nexus.internal.httpclient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.transaction.Transactional;

/**
 * MyBatis {@link HttpClientConfigurationStore} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class HttpClientConfigurationStoreImpl
    extends ConfigStoreSupport<HttpClientConfigurationDAO>
    implements HttpClientConfigurationStore
{
  @Inject
  public HttpClientConfigurationStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public HttpClientConfiguration newConfiguration() {
    return new HttpClientConfigurationData();
  }

  @Transactional
  @Override
  public HttpClientConfiguration load() {
    return dao().get().orElse(null);
  }

  @Transactional
  @Override
  public void save(final HttpClientConfiguration configuration) {
    postCommitEvent(HttpClientConfigurationChanged::new);

    dao().set((HttpClientConfigurationData) configuration);
  }
}
