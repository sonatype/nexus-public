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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Plugin;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UT for {@link MetadataUpdater}
 *
 * @since 3.0
 */
public class MetadataUpdaterTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private MavenFacet mavenFacet;

  @Mock
  private Content content;

  @Mock
  private StorageTx tx;

  @Mock
  private AttributesMap contentAttributes;

  private final Map<HashAlgorithm, HashCode> hashes = ImmutableMap.of(
      HashAlgorithm.SHA1, HashAlgorithm.SHA1.function().hashString("sha1", StandardCharsets.UTF_8),
      HashAlgorithm.MD5, HashAlgorithm.MD5.function().hashString("md5", StandardCharsets.UTF_8)
  );

  private final MavenPath mavenPath = new Maven2MavenPathParser().parsePath("/foo/bar");

  private MetadataUpdater testSubject;

  @Before
  public void prepare() {
    when(contentAttributes.require(Content.CONTENT_HASH_CODES_MAP, Content.T_CONTENT_HASH_CODES_MAP)).thenReturn(hashes);
    when(content.getAttributes()).thenReturn(contentAttributes);

    when(repository.getName()).thenReturn("name");
    when(repository.facet(eq(MavenFacet.class))).thenReturn(mavenFacet);
    this.testSubject = new MetadataUpdater(true, repository);
  }

  @Test
  public void updateWithNonExisting() throws IOException {
    when(mavenFacet.get(mavenPath)).thenReturn(null, content);
    UnitOfWork.beginBatch(tx);
    try {
      testSubject.update(mavenPath, Maven2Metadata.newGroupLevel(DateTime.now(), new ArrayList<Plugin>()));
    }
    finally {
      UnitOfWork.end();
    }
    verify(tx, times(1)).commit();
    verify(mavenFacet, times(2)).get(eq(mavenPath));
    verify(mavenFacet, times(1)).put(eq(mavenPath), any(Payload.class));
  }

  @Test
  public void updateWithExisting() throws IOException {
    when(mavenFacet.get(mavenPath)).thenReturn(
        new Content(
            new StringPayload("<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata><groupId>group</groupId></metadata>",
                "text/xml")), content);
    UnitOfWork.beginBatch(tx);
    try {
      testSubject.update(mavenPath, Maven2Metadata.newGroupLevel(DateTime.now(), new ArrayList<Plugin>()));
    }
    finally {
      UnitOfWork.end();
    }
    verify(tx, times(1)).commit();
    verify(mavenFacet, times(1)).get(eq(mavenPath));
    verify(mavenFacet, times(0)).put(eq(mavenPath), any(Payload.class));
  }

  @Test
  public void updateWithExistingCorrupted() throws IOException {
    when(mavenFacet.get(mavenPath)).thenReturn(
        new Content(new StringPayload("ThisIsNotAnXml", "text/xml")), content);
    UnitOfWork.beginBatch(tx);
    try {
      testSubject.update(mavenPath, Maven2Metadata.newGroupLevel(DateTime.now(), new ArrayList<Plugin>()));
    }
    finally {
      UnitOfWork.end();
    }
    verify(tx, times(1)).commit();
    verify(mavenFacet, times(2)).get(eq(mavenPath));
    verify(mavenFacet, times(1)).put(eq(mavenPath), any(Payload.class));
  }

  @Test
  public void replaceWithExistingCorrupted() throws IOException {
    when(mavenFacet.get(mavenPath)).thenReturn(
        new Content(new StringPayload("ThisIsNotAnXml", "text/xml")), content);
    UnitOfWork.beginBatch(tx);
    try {
      testSubject.replace(mavenPath, Maven2Metadata.newGroupLevel(DateTime.now(), new ArrayList<Plugin>()));
    }
    finally {
      UnitOfWork.end();
    }
    verify(tx, times(1)).commit();
    verify(mavenFacet, times(2)).get(eq(mavenPath));
    verify(mavenFacet, times(1)).put(eq(mavenPath), any(Payload.class));
  }

  @Test
  public void replaceWithUnchangedExisting() throws IOException {
    when(mavenFacet.get(mavenPath)).thenReturn(
        new Content(
            new StringPayload("<?xml version=\"1.0\" encoding=\"UTF-8\"?><metadata></metadata>",
                "text/xml")), content);
    UnitOfWork.beginBatch(tx);
    try {
      testSubject.replace(mavenPath, Maven2Metadata.newGroupLevel(DateTime.now(), new ArrayList<Plugin>()));
    }
    finally {
      UnitOfWork.end();
    }
    verify(tx, times(1)).commit();
    verify(mavenFacet, times(1)).get(eq(mavenPath));
    verify(mavenFacet, times(0)).put(eq(mavenPath), any(Payload.class));
  }

  @Test
  public void delete() throws IOException {
    testSubject.delete(mavenPath);
    verify(tx, times(0)).commit();
    verify(mavenFacet, times(0)).get(eq(mavenPath));
    verify(mavenFacet, times(0)).put(eq(mavenPath), any(Payload.class));
    verify(mavenFacet, times(1))
        .delete(eq(mavenPath), eq(mavenPath.hash(HashType.SHA1)), eq(mavenPath.hash(HashType.MD5)));
  }
}
