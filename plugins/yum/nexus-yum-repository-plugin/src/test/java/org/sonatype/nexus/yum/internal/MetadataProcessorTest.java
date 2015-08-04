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
package org.sonatype.nexus.yum.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.litmus.testsupport.junit.TestDataRule;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.DiffMatchers.equalTo;

/**
 * {@link MetadataProcessor} UTs.
 *
 * @since 2.11
 */
public class MetadataProcessorTest
    extends TestSupport
{
  private static final String STORAGE = "/home/centos/sonatype/nexus-bundles/assemblies/nexus-pro/target/nexus/sonatype-work/nexus/storage/";

  @Rule
  public TestDataRule testData = new TestDataRule(util.resolveFile("src/test/ut-resources"));

  private Map<String, byte[]> storage = Maps.newHashMap();

  @Test
  public void verifyProcessAfterMerge()
      throws Exception
  {
    Repository repository = mock(Repository.class);
    mockRepository(repository, "repo3");

    MetadataProcessor.processMergedMetadata(
        repository,
        Arrays.asList(new File(STORAGE + "releases"), new File(STORAGE + "thirdparty"))
    );

    ArgumentCaptor<ResourceStoreRequest> deleteRequests = ArgumentCaptor.forClass(ResourceStoreRequest.class);
    verify(repository).deleteItem(eq(false), deleteRequests.capture());

    // verify that old primary.xml was deleted
    assertThat(deleteRequests.getAllValues().get(0).getRequestPath(), is(
        "/repodata/b7bad82bc89d0c651f8aeefffa3b43e69725c6fc0cc4dd840ecb999698861428-primary.xml.gz"
    ));

    // check that primary.xml is stored
    byte[] primaryBytes = storage.get(
        "/repodata/2fefc2f0541419d2dcf3fc73d2b8db1cc5782d522443b51b954c4e94d5a8c001-primary.xml.gz"
    );
    assertThat(primaryBytes, is(notNullValue()));

    // check that repomd.xml is stored
    byte[] repomdBytes = storage.get(
        "/repodata/repomd.xml"
    );
    assertThat(repomdBytes, is(notNullValue()));

    // compare primary.xml content
    try (InputStream primaryIn = new GZIPInputStream(new ByteArrayInputStream(primaryBytes))) {
      assertThat(
          IOUtils.toString(primaryIn),
          is(equalTo(readFileToString(testData.resolveFile("repo3-result/repodata/primary.xml"))))
      );
    }

    // compare repomd.xml content
    assertThat(
        IOUtils.toString(new ByteArrayInputStream(repomdBytes)),
        is(equalTo(readFileToString(testData.resolveFile("repo3-result/repodata/repomd.xml"))))
    );
  }

  @Test
  public void verifyProcessAfterProxy()
      throws Exception
  {
    ProxyRepository repository = mock(ProxyRepository.class);
    mockRepository(repository, "repo5");
    when(repository.getRemoteUrl()).thenReturn("http://localhost:8082/nexus/content/repositories/thirdparty");

    MetadataProcessor.processProxiedMetadata(repository);

    ArgumentCaptor<ResourceStoreRequest> deleteRequests = ArgumentCaptor.forClass(ResourceStoreRequest.class);
    verify(repository).deleteItem(eq(false), deleteRequests.capture());

    // verify that old primary.xml was deleted
    assertThat(deleteRequests.getAllValues().get(0).getRequestPath(), is(
        "/repodata/57a84398efb9478dd2fc2d23467b55479939ff3a48f21d0beacd0b5849713f48-primary.xml.gz"
    ));

    // check that primary.xml is stored
    byte[] primaryBytes = storage.get(
        "/repodata/8ef31e4bdca995f6fe5b7a63387ddc60c718175ca6eaec9d4cadbefb81fd5828-primary.xml.gz"
    );
    assertThat(primaryBytes, is(notNullValue()));

    // check that repomd.xml is stored
    byte[] repomdBytes = storage.get(
        "/repodata/repomd.xml"
    );
    assertThat(repomdBytes, is(notNullValue()));

    // compare primary.xml content
    try (InputStream primaryIn = new GZIPInputStream(new ByteArrayInputStream(primaryBytes))) {
      assertThat(
          IOUtils.toString(primaryIn),
          is(equalTo(readFileToString(testData.resolveFile("repo5-result/repodata/primary.xml"))))
      );
    }

    // compare repomd.xml content
    assertThat(
        IOUtils.toString(new ByteArrayInputStream(repomdBytes)),
        is(equalTo(readFileToString(testData.resolveFile("repo5-result/repodata/repomd.xml"))))
    );
  }

  private void mockRepository(final Repository repository, final String repoDir)
      throws Exception
  {
    when(repository.getId()).thenReturn("test");
    when(repository.retrieveItem(eq(false), any(ResourceStoreRequest.class))).thenAnswer(new Answer<StorageFileItem>()
    {
      @Override
      public StorageFileItem answer(final InvocationOnMock invocationOnMock) throws Throwable {
        ResourceStoreRequest request = (ResourceStoreRequest) invocationOnMock.getArguments()[1];
        StorageFileItem storageFileItem = mock(StorageFileItem.class);
        InputStream in;
        if (storage.containsKey(request.getRequestPath())) {
          in = new ByteArrayInputStream(storage.get(request.getRequestPath()));
        }
        else {
          in = new FileInputStream(testData.resolveFile(repoDir + request.getRequestPath()));
        }
        when(storageFileItem.getInputStream()).thenReturn(in);
        return storageFileItem;
      }
    });
    doAnswer(new Answer()
    {
      @Override
      public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
        StorageFileItem item = (StorageFileItem) invocationOnMock.getArguments()[1];
        storage.put(item.getPath(), IOUtils.toByteArray(item.getInputStream()));
        return null;
      }
    }).when(repository).storeItem(eq(false), Mockito.any(StorageItem.class));
  }

}
