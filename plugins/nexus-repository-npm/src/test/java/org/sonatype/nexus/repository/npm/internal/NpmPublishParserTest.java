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
package org.sonatype.nexus.repository.npm.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmPublishParserTest
    extends TestSupport
{
  static final String EXPECTED_SHA1 = "270d96f7bec8561bf7cb45982876f6990a7983e1";

  static final String BLOB_ID = "blob-id";

  static final List<HashAlgorithm> HASH_ALGORITHMS = Collections.singletonList(HashAlgorithm.SHA1);

  static final String NO_USER = null;

  final JsonFactory jsonFactory = new JsonFactory();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  TempBlob tempBlob;

  @Mock
  Blob blob;

  @Mock
  BlobId blobId;

  @Mock
  StorageFacet storageFacet;

  String contentHash;

  @Before
  public void setUp() {
    when(tempBlob.getBlob()).thenReturn(blob);
    when(blob.getId()).thenReturn(blobId);
    when(blobId.toString()).thenReturn("blob-id");
    when(storageFacet.createTempBlob(any(InputStream.class), any(Iterable.class))).thenAnswer(invocation -> {
      byte[] content = ByteStreams.toByteArray((InputStream) invocation.getArguments()[0]);
      Hasher hasher = Hashing.sha1().newHasher().putBytes(content);
      contentHash = hasher.hash().toString();
      return tempBlob;
    });
  }

  @Test
  public void parsePublishJson() throws Exception {
    String file = "publish.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, NO_USER);
  }

  @Test
  public void parseDeleteJson() throws Exception {
    String file = "delete.json";
    String url = "http://localhost:10004/repository/deletePackage1x/deletePackage1x/-/deletePackage1x-1.0.tgz";
    String name = "deletePackage1x";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, NO_USER);
  }

  @Test
  public void parsePublishLocalJson() throws Exception {
    String file = "publish-local.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, NO_USER);
  }

  @Test
  public void parsePublishLocalJsonWithUser() throws Exception {
    String file = "publish-local.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, "bob");
  }

  @Test
  public void parsePublishLocalJsonMaintainerNotArray() throws Exception {
    String file = "publish-local-maintainer-not-array.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, NO_USER);
  }

  @Test
  public void parsePublishLocalJsonMaintainerNotArrayWithUser() throws Exception {
    String file = "publish-local-maintainer-not-array.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, "bob");
  }

  @Test
  public void parsePublishLocalJsonMaintainerShortenedArray() throws Exception {
    String file = "publish-local-maintainer-shortened-array.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, NO_USER);
  }

  @Test
  public void parsePublishLocalJsonMaintainerShortenedArrayWithUser() throws Exception {
    String file = "publish-local-maintainer-shortened-array.json";
    String url = "http://localhost:10004/repository/search/foo/-/foo-1.0.tgz";
    String name = "foo";
    String version = "1.0";
    parseFileAndAssertContents(file, url, name, version, "bob");
  }


  @Test
  public void parsePublishLocalJsonWithUserToken() throws Exception {
    String file = "publish-local-usertoken.json";
    String name = "admin";
    parseFileAndAssertMaintainers(file, name);
  }

  @Test
  public void cleanUpOnFailure() throws Exception {
    try (InputStream in = getClass().getResourceAsStream("broken.json")) {
      try (JsonParser jsonParser = jsonFactory.createParser(in)) {
        NpmPublishParser underTest = new NpmPublishParser(jsonParser, storageFacet, HASH_ALGORITHMS);
        underTest.parse(NO_USER);
        fail(); // exception should be thrown on parse
      }
    }
    catch (IOException e) {
      // expected exception
    }
    verify(tempBlob).close();
  }

  @Test
  public void throwExceptionOnInvalidUtf8Content() throws Exception {
    exception.expectMessage("Invalid UTF-8");
    exception.expect(JsonParseException.class);
    try (InputStream in = new ByteArrayInputStream("{\"name\":\"foo\",\"author\":\"bé\"}".getBytes(ISO_8859_1))) {
      try (JsonParser jsonParser = jsonFactory.createParser(in)) {
        NpmPublishParser underTest = new NpmPublishParser(jsonParser, storageFacet, HASH_ALGORITHMS);
        underTest.parse(NO_USER);
        fail(); // exception should be thrown on parse
      }
    }
  }

  @Test
  public void correctlyHandleExpectedISO_8859_1Content() throws Exception {
    try (InputStream in = new ByteArrayInputStream("{\"name\":\"foo\",\"author\":\"bé\"}".getBytes(ISO_8859_1))) {
      try (InputStreamReader reader = new InputStreamReader(in, ISO_8859_1)) {
        try (JsonParser jsonParser = jsonFactory.createParser(reader)) {
          NpmPublishParser underTest = new NpmPublishParser(jsonParser, storageFacet, HASH_ALGORITHMS);
          underTest.parse(NO_USER);
        }
      }
    }
  }

  private void parseFileAndAssertMaintainers(final String file, final String name)
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream(file)) {
      try (JsonParser jsonParser = jsonFactory.createParser(in)) {
        NpmPublishParser underTest = new NpmPublishParser(jsonParser, storageFacet, HASH_ALGORITHMS);
        try (NpmPublishRequest request = underTest.parse(name)) {
          assertMaintainers(request, name);
        }
      }
    }
  }

  private void assertMaintainers(final NpmPublishRequest request,
                                 final String name)
  {
    NestedAttributesMap packageRoot = request.getPackageRoot();
    NestedAttributesMap versions = packageRoot.child("versions");

    assertMaintainer(name, packageRoot);
    assertMaintainer(name, versions.child("1.0"));
  }

  private void assertMaintainer(final String name, final NestedAttributesMap packageRoot) {
    Map<String, Object> map = (Map<String, Object>) packageRoot.get("maintainers", ArrayList.class).get(0);
    assertThat(map.get("name"), is(name));
    assertThat(packageRoot.child("_npmUser").get("name", String.class), is(name));
  }

  private void parseFileAndAssertContents(final String file, final String url, final String name, final String version, final String currentUser)
      throws IOException
  {
    try (InputStream in = getClass().getResourceAsStream(file)) {
      try (JsonParser jsonParser = jsonFactory.createParser(in)) {
        NpmPublishParser underTest = new NpmPublishParser(jsonParser, storageFacet, HASH_ALGORITHMS);
        try (NpmPublishRequest request = underTest.parse(currentUser)) {
          assertRequestContents(request, name, version, url);
        }
      }
    }
  }

  private void assertRequestContents(final NpmPublishRequest request,
                                     final String name,
                                     final String version,
                                     final String url)
  {
    NestedAttributesMap packageRoot = request.getPackageRoot();
    assertThat(packageRoot.get("name"), is(name));
    assertThat(packageRoot.get("description"), is(name));
    assertThat(packageRoot.get("version"), is("1.0"));

    NestedAttributesMap versions = packageRoot.child("versions");
    assertThat(versions.size(), is(1));
    assertThat(versions.child("1.0").get("name"), is(name));
    assertThat(versions.child("1.0").get("version"), is("1.0"));
    assertThat(versions.child("1.0").child("dist").get("tarball"), is(url));
    assertThat(versions.child("1.0").child("dist").get("shasum"), is("270d96f7bec8561bf7cb45982876f6990a7983e1"));

    NestedAttributesMap distTags = packageRoot.child("dist-tags");
    assertThat(distTags.size(), is(1));
    assertThat(distTags.get("latest"), is("1.0"));

    NestedAttributesMap attachments = packageRoot.child("_attachments");
    assertThat(attachments.size(), is(1));
    assertThat(attachments.child(name + "-" + version + ".tgz").get("content_type"), is("application/gzip"));
    assertThat(attachments.child(name + "-" + version + ".tgz").get("data"), is(BLOB_ID));
    assertThat(attachments.child(name + "-" + version + ".tgz").get("length"), is(new BigInteger("447")));

    assertThat(request.requireBlob(BLOB_ID), is(notNullValue()));
    assertThat(contentHash, is(EXPECTED_SHA1));
  }
}
