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

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.crypto.secrets.SecretData;
import org.sonatype.nexus.datastore.api.DataAccess;

import org.apache.ibatis.annotations.Param;

public interface SecretsDAO
    extends DataAccess
{
  /**
   * Store a record of an encrypted secret. This store is not expected to encrypt the secret, it should be provided
   * in an encrypted form.
   *
   * @param purpose the purpose of the stored key, e.g. LDAP
   * @param keyId   the identifier for the key (not the key itself) which was used to encrypt the secret
   * @param secret  the previously encrypted secret to store
   * @param userId  the user if known who initiated the request
   *
   * @return the id for the persisted record
   */
  int create(
      @Param("purpose") String purpose,
      @Param("keyId") String keyId,
      @Param("secret") String secret,
      @Nullable @Param("userId") String userId);

  /**
   * Deletes a secret from the store by its identifier.
   *
   * @param id the internal identifier for the persisted secret
   * @return {@code true} if a record was removed, {@code false} otherwise.
   */
  int delete(@Param("id") int id);

  /**
   * Reads a secret from the store by its identifier.
   *
   * @param id the id of the secret
   *
   * @return the stored encrypted secret data
   */
  Optional<SecretData> read(@Param("id") int id);

  /**
   * Updates a record by its identifier with a new keyId and secret.
   *
   * @param id     the identifier of the secret
   * @param keyId  the identifier for the key (not the key itself) which was used to encrypt the secret
   * @param secret the previously encrypted secret to store
   * @return the id for the updated record
   */
  int update(@Param("id") int id, @Param("keyId") String keyId, @Param("secret") String secret);

  /**
   * Browse all records stored in the store.
   *
   * @param continuationToken a continuation token provided by a previous response, or {@code null}
   * @param limit             a limit to the number of tokens provided in the page
   *
   * @return a continuation containing the page of records.
   */
  Continuation<SecretData> browse(
      @Nullable @Param("continuationToken") String continuationToken,
      @Param("limit") int limit);
}
