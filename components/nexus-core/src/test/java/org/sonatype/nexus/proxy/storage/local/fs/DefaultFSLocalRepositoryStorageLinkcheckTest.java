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
package org.sonatype.nexus.proxy.storage.local.fs;

import java.io.File;

import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.item.DefaultLinkPersister;
import org.sonatype.nexus.proxy.item.DefaultRepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.storage.local.DefaultLocalStorageContext;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.locks.LocalResourceLockFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultFSLocalRepositoryStorageLinkcheckTest
    extends TestSupport
{
  @Mock
  private Wastebasket wastebasket;

  @Mock
  private MimeSupport mimeSupport;

  @Mock
  private Repository repository;

  @Mock
  private AttributesHandler attributesHandler;

  @Mock
  private RepositoryRegistry repositoryRegistry;

  private RepositoryItemUidFactory repositoryItemUidFactory;

  private LinkPersister linkPersister;

  private File baseDir;

  private DefaultFSLocalRepositoryStorage subject;

  @Before
  public void prepare() throws Exception {
    when(mimeSupport.guessMimeTypeFromPath(any(MimeRulesSource.class), Mockito.anyString())).thenReturn(
        "text/plain");

    baseDir = util.createTempDir();
    final LocalStorageContext localStorageContext = new DefaultLocalStorageContext(null);
    localStorageContext.putContextObject(DefaultFSLocalRepositoryStorage.BASEDIR_FILE, baseDir);

    // repository
    when(repository.getId()).thenReturn("test");
    when(repository.getAttributesHandler()).thenReturn(attributesHandler);
    when(repository.getLocalStorageContext()).thenReturn(localStorageContext);
    when(repository.getRepositoryKind()).thenReturn(new DefaultRepositoryKind(HostedRepository.class, null));
    when(repository.getLocalUrl()).thenReturn(baseDir.toURI().toString());
    when(repository.getRepositoryItemUidAttributeManager()).thenReturn(mock(RepositoryItemUidAttributeManager.class));

    when(repositoryRegistry.getRepository(anyString())).thenReturn(repository);
    this.repositoryItemUidFactory = new DefaultRepositoryItemUidFactory(mock(EventBus.class), repositoryRegistry,
        new LocalResourceLockFactory());
    when(repository.createUid(anyString())).thenAnswer(new Answer<RepositoryItemUid>()
    {
      @Override
      public RepositoryItemUid answer(final InvocationOnMock invocationOnMock) throws Throwable {
        return repositoryItemUidFactory.createUid(repository, (String) invocationOnMock.getArguments()[0]);
      }
    });

    this.linkPersister = new DefaultLinkPersister(repositoryItemUidFactory);

    subject = new DefaultFSLocalRepositoryStorage(wastebasket, linkPersister, mimeSupport, new DefaultFSPeer());
  }

  @Test
  public void storeLinkWithLinkApi() throws Exception {
    final DefaultStorageLinkItem link = new DefaultStorageLinkItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, repositoryItemUidFactory.createUid(repository, "/link/target"));
    subject.storeItem(repository, link);
    // verify work is done ok by retrieving it
    final StorageItem retrievedLink = subject.retrieveItem(repository, new ResourceStoreRequest("/link/path"));
    assertThat(retrievedLink, notNullValue());
    assertThat(retrievedLink, instanceOf(StorageLinkItem.class));
    assertThat(retrievedLink.getPath(), equalTo("/link/path"));
    assertThat(((StorageLinkItem) retrievedLink).getTarget(), notNullValue());
    assertThat(((StorageLinkItem) retrievedLink).getTarget().getRepository().getId(), equalTo(repository.getId()));
    assertThat(((StorageLinkItem) retrievedLink).getTarget().getPath(), equalTo("/link/target"));
  }

  @Test(expected = UnsupportedStorageOperationException.class)
  public void storeLinkWithoutLinkApiReusableContent() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("LINK to test:/some/path"));
    subject.storeItem(repository, file);
  }

  @Test(expected = UnsupportedStorageOperationException.class)
  public void storeLinkWithoutLinkApiNonReusableContent() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("LINK to test:/some/path")
    {
      @Override
      public boolean isReusable() {
        return false;
      }
    });
    subject.storeItem(repository, file);
  }

  @Test(expected = UnsupportedStorageOperationException.class)
  public void storeLinkWithoutLinkApiReusableContentUnknownLength() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("LINK to test:/some/path"){
      @Override
      public long getLength() { return -1; }
    });
    subject.storeItem(repository, file);
  }

  @Test(expected = UnsupportedStorageOperationException.class)
  public void storeLinkWithoutLinkApiNonReusableContentUnknownLength() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("LINK to test:/some/path")
    {
      @Override
      public boolean isReusable() {
        return false;
      }

      @Override
      public long getLength() { return -1; }
    });
    subject.storeItem(repository, file);
  }

  @Test
  public void storeNonLinkWithoutLinkApiReusableContentUnknownLengthOneByte() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("b"){
      @Override
      public long getLength() { return -1; }
    });
    subject.storeItem(repository, file);
  }

  @Test
  public void storeNonLinkWithoutLinkApiNonReusableContentUnknownLengthOneByte() throws Exception {
    final DefaultStorageFileItem file = new DefaultStorageFileItem(repository, new ResourceStoreRequest("/link/path"),
        true, true, new StringContentLocator("b")
    {
      @Override
      public boolean isReusable() {
        return false;
      }

      @Override
      public long getLength() { return -1; }
    });
    subject.storeItem(repository, file);
  }

  @Test
  public void retrieveExistingLinkProper() throws Exception {
    // link has no relevant attributes
    final File linkPath = new File(baseDir, "link/path");
    linkPath.getParentFile().mkdirs();
    Files.asCharSink(linkPath, Charsets.UTF_8).write("LINK to test:/link/target");
    final StorageItem retrievedLink = subject.retrieveItem(repository, new ResourceStoreRequest("/link/path"));
    assertThat(retrievedLink, notNullValue());
    assertThat(retrievedLink, instanceOf(StorageLinkItem.class));
    assertThat(retrievedLink.getPath(), equalTo("/link/path"));
    assertThat(((StorageLinkItem) retrievedLink).getTarget(), notNullValue());
    assertThat(((StorageLinkItem) retrievedLink).getTarget().getRepository().getId(), equalTo(repository.getId()));
    assertThat(((StorageLinkItem) retrievedLink).getTarget().getPath(), equalTo("/link/target"));
  }

  @Test
  public void retrieveExistingProxiedLink() throws Exception {
    // proxied ones has remoteUrl
    doAnswer(new Answer<Void>()
    {
      @Override
      public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
        final StorageItem item = (StorageItem) invocationOnMock.getArguments()[0];
        item.getRepositoryItemAttributes().setRemoteUrl("http://sonatype.org/");
        return null;
      }
    }).when(attributesHandler).fetchAttributes(any(StorageItem.class));
    final File linkPath = new File(baseDir, "link/path");
    linkPath.getParentFile().mkdirs();
    Files.asCharSink(linkPath, Charsets.UTF_8).write("LINK to test:/link/target");
    final StorageItem retrievedLink = subject.retrieveItem(repository, new ResourceStoreRequest("/link/path"));
    assertThat(retrievedLink, notNullValue());
    // is NOT a link, is a plain FILE
    assertThat(retrievedLink, instanceOf(StorageFileItem.class));
  }

  @Test
  public void retrieveExistingUploadedLink() throws Exception {
    // uploaded ones has remote IP
    doAnswer(new Answer<Void>()
    {
      @Override
      public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
        final StorageItem item = (StorageItem) invocationOnMock.getArguments()[0];
        item.getRepositoryItemAttributes().put(AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1");
        return null;
      }
    }).when(attributesHandler).fetchAttributes(any(StorageItem.class));
    final File linkPath = new File(baseDir, "link/path");
    linkPath.getParentFile().mkdirs();
    Files.asCharSink(linkPath, Charsets.UTF_8).write("LINK to test:/link/target");
    final StorageItem retrievedLink = subject.retrieveItem(repository, new ResourceStoreRequest("/link/path"));
    assertThat(retrievedLink, notNullValue());
    // is NOT a link, is a plain FILE
    assertThat(retrievedLink, instanceOf(StorageFileItem.class));
  }
}
