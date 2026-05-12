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
package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Map;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditRecorder;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreCreatedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreDeletedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreStartedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreStoppedEvent;
import org.sonatype.nexus.blobstore.api.BlobStoreUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BlobStoreAuditorTest
{
  private static final String BLOB_STORE_NAME = "test-blobstore";

  private static final String BLOB_STORE_TYPE = "File";

  @Mock
  private AuditRecorder auditRecorder;

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  private BlobStoreAuditor underTest;

  @Before
  public void setup() {
    underTest = new BlobStoreAuditor();
    // Use reflection to set the audit recorder since it's protected
    try {
      java.lang.reflect.Field field = underTest.getClass().getSuperclass().getDeclaredField("auditRecorder");
      field.setAccessible(true);
      field.set(underTest, auditRecorder);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to set audit recorder", e);
    }

    // Enable recording
    when(auditRecorder.isEnabled()).thenReturn(true);

    // Set up mock blobstore
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn(BLOB_STORE_NAME);
    when(blobStoreConfiguration.getType()).thenReturn(BLOB_STORE_TYPE);
  }

  @Test
  public void testCreatedEvent_recordsAuditData() {
    BlobStoreCreatedEvent event = new BlobStoreCreatedEvent(blobStore);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is(BlobStoreAuditor.DOMAIN));
    assertThat(auditData.getType(), is("created"));
    assertThat(auditData.getContext(), is(BLOB_STORE_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("name"));
    assertThat(attributes.get("name"), is(BLOB_STORE_NAME));
    assertThat(attributes, hasKey("type"));
    assertThat(attributes.get("type"), is(BLOB_STORE_TYPE));
  }

  @Test
  public void testUpdatedEvent_recordsAuditData() {
    BlobStoreUpdatedEvent event = new BlobStoreUpdatedEvent(blobStore);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is(BlobStoreAuditor.DOMAIN));
    assertThat(auditData.getType(), is("updated"));
    assertThat(auditData.getContext(), is(BLOB_STORE_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("name"));
    assertThat(attributes.get("name"), is(BLOB_STORE_NAME));
    assertThat(attributes, hasKey("type"));
    assertThat(attributes.get("type"), is(BLOB_STORE_TYPE));
  }

  @Test
  public void testDeletedEvent_recordsAuditData() {
    BlobStoreDeletedEvent event = new BlobStoreDeletedEvent(blobStore);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is(BlobStoreAuditor.DOMAIN));
    assertThat(auditData.getType(), is("deleted"));
    assertThat(auditData.getContext(), is(BLOB_STORE_NAME));

    Map<String, Object> attributes = auditData.getAttributes();
    assertThat(attributes, hasKey("name"));
    assertThat(attributes.get("name"), is(BLOB_STORE_NAME));
    assertThat(attributes, hasKey("type"));
    assertThat(attributes.get("type"), is(BLOB_STORE_TYPE));
  }

  @Test
  public void testStartedEvent_recordsAuditData() {
    BlobStoreStartedEvent event = new BlobStoreStartedEvent(blobStore);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is(BlobStoreAuditor.DOMAIN));
    assertThat(auditData.getType(), is("started"));
    assertThat(auditData.getContext(), is(BLOB_STORE_NAME));
  }

  @Test
  public void testStoppedEvent_recordsAuditData() {
    BlobStoreStoppedEvent event = new BlobStoreStoppedEvent(blobStore);

    underTest.on(event);

    ArgumentCaptor<AuditData> captor = ArgumentCaptor.forClass(AuditData.class);
    verify(auditRecorder).record(captor.capture());

    AuditData auditData = captor.getValue();
    assertThat(auditData.getDomain(), is(BlobStoreAuditor.DOMAIN));
    assertThat(auditData.getType(), is("stopped"));
    assertThat(auditData.getContext(), is(BLOB_STORE_NAME));
  }
}
