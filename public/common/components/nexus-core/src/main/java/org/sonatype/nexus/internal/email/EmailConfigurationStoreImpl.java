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
package org.sonatype.nexus.internal.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * MyBatis {@link EmailConfigurationStore} implementation.
 *
 * @since 3.21
 */
@Primary
@Component
@Qualifier("mybatis")
public class EmailConfigurationStoreImpl
    extends ConfigStoreSupport<EmailConfigurationDAO>
    implements EmailConfigurationStore
{
  @Autowired
  public EmailConfigurationStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public EmailConfiguration newConfiguration() {
    return new EmailConfigurationData();
  }

  @Transactional
  @Override
  public EmailConfiguration load() {
    return dao().get().orElse(null);
  }

  @Transactional
  @Override
  public void save(final EmailConfiguration configuration) {
    postCommitEvent(EmailConfigurationChanged::new);
    dao().set((EmailConfigurationData) configuration);
  }
}
