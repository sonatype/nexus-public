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
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.security.apikey.ApiKeyInternal;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyData;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreImpl;
import org.sonatype.nexus.internal.security.apikey.store.ApiKeyStoreV2Impl;
import org.sonatype.nexus.kv.GlobalKeyValueStore;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.internal.security.apikey.ApiKeyServiceImpl.MIGRATION_COMPLETE;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiKeyToSecretsTaskTest
{
  private static final String DOMAIN = "NuGet";

  private static final OffsetDateTime CREATED = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  // asserted against the literal, not ApiKeyToSecretsTask.EPOCH, so a regression of the constant itself is caught
  private static final OffsetDateTime EXPECTED_EPOCH = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  @Mock
  private ApiKeyStoreImpl apiKeyStoreV1;

  @Mock
  private ApiKeyStoreV2Impl apiKeyStoreV2;

  @Mock
  private GlobalKeyValueStore kv;

  private ApiKeyToSecretsTask underTest;

  @Before
  public void setup() {
    // interval 0 keeps synchronizationDelayMs at 0 so the catch-up loop is skipped
    underTest = new ApiKeyToSecretsTask(apiKeyStoreV1, apiKeyStoreV2, 0, kv);
  }

  private static ApiKeyData record(final String token) {
    ApiKeyData data = new ApiKeyData();
    data.setDomain(DOMAIN);
    data.setPrincipals(new SimplePrincipalCollection("bob", "ldap"));
    data.setApiKey(token.toCharArray());
    data.setCreated(CREATED);
    return data;
  }

  @Test
  public void migratesEveryRecordAndMarksTheMigrationComplete() throws Exception {
    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(record("token-one")))
        .thenReturn(List.of());

    underTest.execute();

    verify(apiKeyStoreV2).persistApiKey(eq(DOMAIN), any(), any(), eq(CREATED));
    verify(kv).setBoolean(MIGRATION_COMPLETE, true);
  }

  @Test
  public void doesNotMarkTheMigrationCompleteWhenARecordFails() {
    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(record("token-one")))
        .thenReturn(List.of());
    doThrow(new RuntimeException("boom"))
        .when(apiKeyStoreV2)
        .persistApiKey(anyString(), any(), any(), any());

    assertThrows(IllegalStateException.class, () -> underTest.execute());

    verify(kv, never()).setBoolean(eq(MIGRATION_COMPLETE), any(Boolean.class));
  }

  @Test
  public void advancesTheCursorToTheLastRecordOfAFullPageBeforeFetchingTheNext() throws Exception {
    List<ApiKeyData> fullPage = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      ApiKeyData record = record("token-" + i);
      record.setCreated(CREATED.plusMinutes(i));
      fullPage.add(record);
    }
    ApiKeyData last = fullPage.get(fullPage.size() - 1);

    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(fullPage)
        .thenReturn(List.of());

    underTest.execute();

    verify(apiKeyStoreV1).browseAllSince(eq(EXPECTED_EPOCH), eq(""), eq(""), eq(100));
    verify(apiKeyStoreV1).browseAllSince(
        eq(last.getCreated()), eq(last.getDomain()), eq(new String(last.getApiKey())), eq(100));
  }

  @Test
  public void startsPagingAtTheEpochSoPre2000RecordsAreMigrated() throws Exception {
    ApiKeyData ancient = record("token-one");
    ancient.setCreated(OffsetDateTime.of(1999, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC));

    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(ancient))
        .thenReturn(List.of());

    underTest.execute();

    verify(apiKeyStoreV1).browseAllSince(eq(EXPECTED_EPOCH), eq(""), eq(""), anyInt());
    verify(apiKeyStoreV2).persistApiKey(eq(DOMAIN), any(), any(), eq(ancient.getCreated()));
  }

  @Test
  public void replacesAnAlreadyMigratedRowThatIsOlderThanItsSource() throws Exception {
    ApiKeyData source = record("token-one");
    ApiKeyInternal existing = mock(ApiKeyInternal.class);
    when(existing.getCreated()).thenReturn(CREATED.minusYears(1));

    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of(source))
        .thenReturn(List.of());
    doThrow(new DuplicateKeyException(new RuntimeException("duplicate")))
        .doNothing()
        .when(apiKeyStoreV2)
        .persistApiKey(anyString(), any(), any(), any());
    when(apiKeyStoreV2.getApiKey(eq(DOMAIN), any())).thenReturn(Optional.of(existing));

    underTest.execute();

    verify(apiKeyStoreV2).deleteApiKey(eq(DOMAIN), any());
    verify(kv).setBoolean(MIGRATION_COMPLETE, true);
  }

  @Test
  public void failsWhenARecordFailsDuringTheSynchronizationWindow() {
    // interval 1 gives a 2 second catch-up window, the smallest the constructor can produce
    ApiKeyToSecretsTask task = new ApiKeyToSecretsTask(apiKeyStoreV1, apiKeyStoreV2, 1, kv);

    when(apiKeyStoreV1.browseAllSince(any(), anyString(), anyString(), anyInt()))
        .thenReturn(List.of()) // the initial scan finds nothing, so the completion flag is set
        .thenReturn(List.of(record("token-one"))) // this row is written during the catch-up window
        .thenReturn(List.of());
    doThrow(new RuntimeException("boom"))
        .when(apiKeyStoreV2)
        .persistApiKey(anyString(), any(), any(), any());

    IllegalStateException thrown = assertThrows(IllegalStateException.class, task::execute);

    assertThat(thrown.getMessage(), containsString("during synchronization"));
    // the flag was already set before the window opened, so the failure has to surface as a task failure
    verify(kv).setBoolean(MIGRATION_COMPLETE, true);
  }
}
