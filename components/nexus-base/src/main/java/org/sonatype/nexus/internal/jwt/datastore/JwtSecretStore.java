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
package org.sonatype.nexus.internal.jwt.datastore;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.security.jwt.JwtSecretChanged;
import org.sonatype.nexus.security.jwt.SecretStore;
import org.sonatype.nexus.transaction.Transactional;

import static org.sonatype.nexus.common.app.FeatureFlags.JWT_ENABLED;

/**
 * Implementation of {@link SecretStore} for datastore.
 *
 * @since 3.38
 */
@Named("mybatis")
@Singleton
@FeatureFlag(name = JWT_ENABLED)
public class JwtSecretStore
    extends ConfigStoreSupport<JwtSecretDAO>
    implements SecretStore
{
  @Inject
  public JwtSecretStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  @Override
  public Optional<String> getSecret() {
    return dao().get();
  }

  @Transactional
  @Override
  public void setSecret(final String secret) {
    postCommitEvent(JwtSecretChanged::new);
    dao().set(secret);
  }

  @Transactional
  @Override
  public void generateNewSecret() {
    String secret = UUID.randomUUID().toString();
    postCommitEvent(JwtSecretChanged::new);
    dao().setIfEmpty(secret);
  }
}
