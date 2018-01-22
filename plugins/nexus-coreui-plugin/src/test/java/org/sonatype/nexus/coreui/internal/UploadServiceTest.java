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
package org.sonatype.nexus.coreui.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.internal.UploadManagerImpl;
import org.sonatype.nexus.repository.view.Payload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class UploadServiceTest
    extends TestSupport
{
  private static final String REPO_NAME = "repo";

  private UploadService component;

  private UploadManager uploadManager;

  private final RepositoryManager repositoryManager = mock(RepositoryManager.class);

  private final UploadHandler handler = mock(UploadHandler.class);

  private final Repository repo = mock(Repository.class);

  @Before
  public void setup() {
    when(repo.getFormat()).thenReturn(new Format("m2")
    {
    });
    when(repo.getType()).thenReturn(new HostedType());
    when(repositoryManager.get(REPO_NAME)).thenReturn(repo);

    UploadDefinition ud = new UploadDefinition("m2", true,
        Arrays.asList(new UploadFieldDefinition("g", false, STRING), new UploadFieldDefinition("v", true, STRING)),
        Arrays.asList(new UploadFieldDefinition("e", false, STRING), new UploadFieldDefinition("c", true, STRING)));
    when(handler.getDefinition()).thenReturn(ud);
    uploadManager = new UploadManagerImpl(Collections.singletonMap("m2", handler));

    component = new UploadService(repositoryManager, uploadManager);
  }

  @Test
  public void testUpload_missingRepositoryName() throws IOException {
    try {
      component.upload(Collections.emptyMap(), Collections.emptyMap());
      fail("Expected exception to be thrown");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage(), is("Missing repositoryName parameter"));
    }
  }

  @Test
  public void testUpload_unknownRepository() throws IOException {
    try {
      component.upload(map("repositoryName", "foo"), Collections.emptyMap());
      fail("Expected exception to be thrown");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage(), is("Specified repository is missing"));
    }
  }

  @Test
  public void testUpload() throws IOException {
    // component field 'v' and asset field 'c' are omitted to ensure optional fields don't trigger an error
    component.upload(map("repositoryName", REPO_NAME, "g", "foo", "e", "jar"),
        map(mockFile("text/plain", 3L, "stuff")));

    ArgumentCaptor<ComponentUpload> captor = ArgumentCaptor.forClass(ComponentUpload.class);
    verify(handler, times(1)).handle(eq(repo), captor.capture());

    ComponentUpload uc = captor.getValue();
    assertThat(uc.getFields(), hasEntry("g", "foo"));

    assertThat(uc.getAssetUploads(), hasSize(1));

    AssetUpload ua = uc.getAssetUploads().get(0);
    assertThat(ua.getFields(), hasEntry("e", "jar"));

    assertPayload(ua.getPayload(), "text/plain", 3L, "stuff");
  }

  @Test
  public void testUpload_multipleAssets() throws IOException {
    component.upload(
        map("repositoryName", REPO_NAME, "g", "foo", "v", "1", "e", "jar", "c", "srcs", "e1", "pom", "c1", "n"),
        map(mockFile("text/plain", 3L, "src"), mockFile("text/xml", 5L, "model")));

    ArgumentCaptor<ComponentUpload> captor = ArgumentCaptor.forClass(ComponentUpload.class);
    verify(handler, times(1)).handle(eq(repo), captor.capture());

    ComponentUpload uc = captor.getValue();
    assertThat(uc.getFields(), hasEntry("g", "foo"));

    assertThat(uc.getAssetUploads(), hasSize(2));

    AssetUpload ua = uc.getAssetUploads().get(0);
    assertThat(ua.getFields(), hasEntry("e", "jar"));
    assertThat(ua.getFields(), hasEntry("c", "srcs"));
    assertPayload(ua.getPayload(), "text/plain", 3L, "src");

    ua = uc.getAssetUploads().get(1);
    assertThat(ua.getFields(), hasEntry("e", "pom"));
    assertThat(ua.getFields(), hasEntry("c", "n"));
    assertPayload(ua.getPayload(), "text/xml", 5L, "model");
  }

  @Test
  public void testCreateSearchTerm() {
    String result = component
        .createSearchTerm(Arrays.asList("foo-x.z/bar/bar", "foo-x.z/bar/foo", "foo-x.z/bar/foo/bar"));

    assertThat(result, is("foo\\-x\\.z\\/bar"));
  }

  private static void assertPayload(final Payload actual,
                                    final String contentType,
                                    final long length,
                                    final String content)
      throws IOException
  {
    assertNotNull(actual);
    assertThat(actual.getContentType(), is(contentType));
    assertThat(actual.getSize(), is(length));
    try (InputStream in = actual.openInputStream()) {
      assertThat(IOUtils.toString(in), is(content));
    }
  }

  private FileItem mockFile(final String contentType, final long length, final String content) throws IOException {
    FileItem fileItem = mock(FileItem.class);
    when(fileItem.getContentType()).thenReturn(contentType);
    when(fileItem.getSize()).thenReturn(length);
    when(fileItem.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    return fileItem;
  }

  private Map<String, FileItem> map(final FileItem... files) {
    Map<String, FileItem> map = new LinkedHashMap<>(); // using linked so tests can expect a deterministic order
    for (int i = 0; i < files.length; i++) {
      map.put("file" + (i > 0 ? i : ""), files[i]);
    }
    return map;
  }

  private Map<String, String> map(final String... strings) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < strings.length; i += 2) {
      map.put(strings[i], strings[i + 1]);
    }
    return map;
  }

}
