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
package org.sonatype.nexus.internal.security.secrets;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

@Named
@Singleton
public class SecretsStoreImpl
    extends ConfigStoreSupport<SecretsDAO>
    implements SecretsStore
{
  @Inject
  public SecretsStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier, SecretsDAO.class);
  }

  @Transactional
  @Override
  public int create(
      final String purpose,
      @Nullable final String keyId,
      final String secret,
      @Nullable final String userId)
  {
    SecretData secretData = new SecretData();
    secretData.setPurpose(purpose);
    secretData.setKeyId(keyId);
    secretData.setSecret(secret);
    secretData.setUserId(userId);
    dao().create(secretData);
    return secretData.getId();
  }

  @Transactional
  @Override
  public boolean delete(final int id) {
    return dao().delete(id) > 0;
  }

  @Transactional
  @Override
  public boolean update(final int id, final String oldSecret, final String keyId, final String secret) {
    return dao().update(id, oldSecret, keyId, secret) > 0;
  }

  @Transactional
  @Override
  public Optional<SecretData> read(final int id) {
    return dao().read(id);
  }

  @Transactional
  @Override
  public boolean existWithDifferentKeyId(final String keyId) {
    return dao().existWithDifferentKeyId(keyId);
  }

  @Transactional
  @Override
  public List<SecretData> fetchWithDifferentKeyId(final String keyId, final int limit) {
    return dao().fetchWithDifferentKeyId(keyId, limit);
  }
}
