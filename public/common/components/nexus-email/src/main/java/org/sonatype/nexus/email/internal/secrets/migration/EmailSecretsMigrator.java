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
package org.sonatype.nexus.email.internal.secrets.migration;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.security.secrets.SecretsMigrator;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EmailSecretsMigrator
    implements SecretsMigrator
{
  private final EmailManager emailManager;

  @Autowired
  public EmailSecretsMigrator(final EmailManager emailManager) {
    this.emailManager = checkNotNull(emailManager);
  }

  @Override
  public void migrate() {
    CancelableHelper.checkCancellation();
    EmailConfiguration configuration = emailManager.getConfiguration();
    Secret password = configuration.getPassword();

    // password not exists or is already migrated
    if (password == null || isPersistedSecret(password)) {
      return;
    }

    // email manager encrypts and handles removal in case of failure
    emailManager.setConfiguration(configuration, new String(password.decrypt()));
  }

}
