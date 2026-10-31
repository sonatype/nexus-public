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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyData;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreImpl;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskSupport;

import org.springframework.beans.factory.annotation.Value;

import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.internal.security.apikey.ApiKeyServiceImpl.MIGRATION_COMPLETE;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A task which is used to migrate from {@code api_key} to {@code api_key_v2} and encrypt using the
 * {@link SecretsService}
 */
@SuppressWarnings("deprecation")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ApiKeyToSecretsTask
    extends TaskSupport
    implements Cancelable
{
  static final String MESSAGE = "Upgrade - moving api keys to v2 table";

  static final String TYPE_ID = "nexus.apikey.secrets";

  /**
   * Lower bound for the migration cursor. The query uses a strict greater-than comparison, so this must sit below
   * every real record. A previous value of "this year, with the year replaced by 2000" silently skipped anything
   * older. OffsetDateTime.MIN is not usable: its year is outside the range a timestamp column accepts.
   */
  static final OffsetDateTime EPOCH = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  private final ApiKeyStoreImpl apiKeyStoreV1;

  private final ApiKeyStoreV2Impl apiKeyStoreV2;

  private final GlobalKeyValueStore kv;

  private final int synchronizationDelayMs;

  @Autowired
  public ApiKeyToSecretsTask(
      @Qualifier("v1") final ApiKeyStoreImpl apiKeyStoreV1,
      @Qualifier("v2") final ApiKeyStoreV2Impl apiKeyStoreV2,
      @Value("${nexus.distributed.events.fetch.interval.seconds:5}") final int interval,
      final GlobalKeyValueStore kv)
  {
    this.apiKeyStoreV1 = requireNonNull(apiKeyStoreV1);
    this.apiKeyStoreV2 = requireNonNull(apiKeyStoreV2);
    this.kv = requireNonNull(kv);
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

    OffsetDateTime last = EPOCH;
    String lastDomain = "";
    String lastToken = "";
    List<String> failedRecords = new ArrayList<>();
    Collection<ApiKeyData> data;
    do {
      data = apiKeyStoreV1.browseAllSince(last, lastDomain, lastToken, pageSize);
      for (ApiKeyData key : data) {
        CancelableHelper.checkCancellation();
        if (!migrateRecord(key)) {
          // the cursor still advances past a failed record: this scan does not retry it. Any failure below aborts
          // the task without setting the completion flag, so the next run rescans from EPOCH and picks it up again.
          failedRecords.add(identify(key));
        }
        last = key.getCreated();
        lastDomain = key.getDomain();
        lastToken = new String(key.getApiKey());
      }
    }
    while (data.size() == pageSize);

    if (!failedRecords.isEmpty()) {
      log.error("Api key migration failed for {} record(s): {}; {} was not set, so reads continue to use the v1 "
          + "table. The task runs again on restart.", failedRecords.size(), failedRecords, MIGRATION_COMPLETE);
      throw new IllegalStateException(
          String.format("Api key migration failed for %d record(s)", failedRecords.size()));
    }

    kv.setBoolean(MIGRATION_COMPLETE, true);

    // We intentionally do not check for cancellation in this section
    final long end = System.currentTimeMillis() + synchronizationDelayMs;
    while (end > System.currentTimeMillis()) {
      data = apiKeyStoreV1.browseAllSince(last, lastDomain, lastToken, pageSize);
      for (ApiKeyData key : data) {
        if (!migrateRecord(key)) {
          failedRecords.add(identify(key));
        }
        last = key.getCreated();
        lastDomain = key.getDomain();
        lastToken = new String(key.getApiKey());
      }

      Thread.sleep(100);
    }

    if (!failedRecords.isEmpty()) {
      log.error("Api key migration completed with {} failed record(s) during the synchronization window: {}; {} "
          + "is already set, so those keys must be investigated.", failedRecords.size(), failedRecords,
          MIGRATION_COMPLETE);
      throw new IllegalStateException(
          String.format("Api key migration failed for %d record(s) during synchronization", failedRecords.size()));
    }

    return null;
  }

  /**
   * Identifies a record for operator troubleshooting without exposing the key itself.
   */
  private static String identify(final ApiKeyData key) {
    return key.getDomain() + ":" + key.getPrimaryPrincipal();
  }

  private boolean migrateRecord(final ApiKeyData key) {
    String primary = key.getPrimaryPrincipal();
    String domain = key.getDomain();
    try {
      log.trace("Migrating {} in {}", primary, domain);
      apiKeyStoreV2.persistApiKey(domain, key.getPrincipals(), key.getApiKey(), key.getCreated());
      return true;
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
      return true;
    }
    catch (Exception e) {
      log.warn("Unable to migrate record for user {} for {}", primary, domain, e);
      return false;
    }
  }
}
