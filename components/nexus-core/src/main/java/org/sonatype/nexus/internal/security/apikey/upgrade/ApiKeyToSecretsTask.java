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
package org.sonatype.nexus.internal.security.apikey.upgrade;

import java.time.OffsetDateTime;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyData;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreImpl;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.internal.security.apikey.ApiKeyServiceImpl.MIGRATION_COMPLETE;

/**
 * A task which is used to migrate from {@code api_key} to {@code api_key_v2} and encrypt using the
 * {@link SecretsService}
 */
@SuppressWarnings("deprecation")
@Named
public class ApiKeyToSecretsTask
    extends TaskSupport
{
  static final String MESSAGE = "Upgrade - moving api keys to v2 table";

  static final String TYPE_ID = "nexus.apikey.secrets";

  private final ApiKeyStoreImpl apiKeyStoreV1;

  private final ApiKeyStoreV2Impl apiKeyStoreV2;

  private final GlobalKeyValueStore kv;

  private final int synchronizationDelayMs;

  @Inject
  public ApiKeyToSecretsTask(
      @Named("v1") final ApiKeyStoreImpl apiKeyStoreV1,
      @Named("v2") final ApiKeyStoreV2Impl apiKeyStoreV2,
      @Named("${nexus.distributed.events.fetch.interval.seconds:-5}") final int interval,
      final GlobalKeyValueStore kv)
  {
    this.apiKeyStoreV1 = checkNotNull(apiKeyStoreV1);
    this.apiKeyStoreV2 = checkNotNull(apiKeyStoreV2);
    this.kv = checkNotNull(kv);
    // We convert this to millis and double it to allow for a good window
    this.synchronizationDelayMs = interval * 2_000;
  }

  @Override
  public String getMessage() {
    return MESSAGE;
  }

  @Override
  protected Object execute() throws Exception {
    final int pageSize = 100;

    OffsetDateTime last = OffsetDateTime.now().withYear(2000); // 2000 for the beginning of time
    Collection<ApiKeyData> data;
    do {
      data = apiKeyStoreV1.browseAllSince(last, pageSize);
      for (ApiKeyData key : data) {
        CancelableHelper.checkCancellation();
        migrateRecord(key);
        last = key.getCreated();
      }
    }
    while (data.size() == pageSize);

    kv.setBoolean(MIGRATION_COMPLETE, true);

    // We intentionally do not check for cancellation in this section
    final long end = System.currentTimeMillis() + synchronizationDelayMs;
    while (end > System.currentTimeMillis()) {
      data = apiKeyStoreV1.browseAllSince(last, pageSize);
      for (ApiKeyData key : data) {
        migrateRecord(key);
        last = key.getCreated();
      }

      Thread.sleep(100);
    }

    return null;
  }

  private void migrateRecord(final ApiKeyData key) {
    String primary = key.getPrimaryPrincipal();
    String domain = key.getDomain();
    try {
      log.trace("Migrating {} in {}", primary, domain);
      apiKeyStoreV2.persistApiKey(domain, key.getPrincipals(), key.getApiKey(), key.getCreated());
    }
    catch (DuplicateKeyException e) {
      log.debug("ApiKey for {} in {} appears to have been migrated.", primary, domain, e);
      apiKeyStoreV2.getApiKey(domain, key.getPrincipals())
          .filter(existingKey -> existingKey.getCreated().isBefore(key.getCreated()))
          .ifPresent(__ -> {
            log.debug("Replacing older key");
            apiKeyStoreV2.deleteApiKey(domain, key.getPrincipals());
            apiKeyStoreV2.persistApiKey(domain, key.getPrincipals(), key.getApiKey(), key.getCreated());
          });
    }
    catch (Exception e) {
      log.warn("Unable to migrate record for user {} for {}", primary, domain, e);
    }
  }
}
