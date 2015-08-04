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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sonatype.nexus.mime.MimeRulesSource;
import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.AttributesHandler;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.LinkPersister;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsItemAttributeMetacontentAttribute;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.DefaultLocalStorageContext;
import org.sonatype.nexus.proxy.storage.local.LocalStorageContext;
import org.sonatype.nexus.proxy.wastebasket.Wastebasket;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultFSLocalRepositoryStorage}
 *
 * @since 2.0
 */
public class DefaultFSLocalRepositoryStorageTest
    extends TestSupport
{
  @Mock
  private Wastebasket wastebasket;

  @Mock
  private MimeSupport mimeSupport;

  @Mock
  private FSPeer fsPeer;

  @Mock
  private LinkPersister linkPersister;

  @Mock
  private Repository repository;

  @Mock
  private RepositoryItemUid repositoryItemUid;

  @Mock
  private AttributesHandler attributesHandler;

  private File baseDir;

  private DefaultFSLocalRepositoryStorage subject;

  @Before
  public void prepare() {
    when(mimeSupport.guessMimeTypeFromPath(Mockito.any(MimeRulesSource.class), Mockito.anyString())).thenReturn(
        "text/plain");

    baseDir = util.createTempDir();
    final LocalStorageContext localStorageContext = new DefaultLocalStorageContext(null);
    localStorageContext.putContextObject(DefaultFSLocalRepositoryStorage.BASEDIR_FILE, baseDir);

    // repository
    when(repository.getId()).thenReturn("test");
    when(repository.createUid(anyString())).thenReturn(repositoryItemUid);
    when(repository.getAttributesHandler()).thenReturn(attributesHandler);
    when(repository.getLocalStorageContext()).thenReturn(localStorageContext);
    when(repository.getRepositoryKind()).thenReturn(new DefaultRepositoryKind(HostedRepository.class, null));
    when(repository.getLocalUrl()).thenReturn(baseDir.toURI().toString());

    subject = new DefaultFSLocalRepositoryStorage(wastebasket, linkPersister, mimeSupport, fsPeer);
  }

  /**
   * Tests listing a directory, when a contained file does NOT exists.
   */
  @Test
  public void testListFilesThrowsItemNotFoundException() throws Exception {
    File repoLocation = new File(util.getBaseDir(), "target/" + getClass().getSimpleName() + "/repo/");

    // the contents of the "valid" directory, only contains a "valid.txt" file
    File validDir = new File(repoLocation, "valid/");
    validDir.mkdirs();
    FileUtils.write(new File(validDir, "valid.txt"), "something valid", "UTF-8");
    Collection<File> validFileCollection = Arrays.asList(validDir.listFiles());

    // the contents of the "invalid" directory, this dir contains a missing file
    File invalidDir = new File(repoLocation, "invalid/");
    invalidDir.mkdirs();
    FileUtils.write(new File(invalidDir, "invalid.txt"), "something valid", "UTF-8");
    List<File> invalidFileCollection = new ArrayList<File>(Arrays.asList(invalidDir.listFiles()));
    invalidFileCollection.add(new File(invalidDir, "missing.txt"));

    // Mocks

    // Mock FSPeer to return the results created above
    when(fsPeer
        .listItems(Mockito.any(Repository.class), Mockito.any(File.class), Mockito.any(ResourceStoreRequest.class),
            eq(validDir))).thenReturn(validFileCollection);
    when(fsPeer
        .listItems(Mockito.any(Repository.class), Mockito.any(File.class), Mockito.any(ResourceStoreRequest.class),
            eq(new File(repoLocation, "invalid/")))).thenReturn(invalidFileCollection);

    // create Repository Mock
    when(repository.getLocalUrl()).thenReturn(repoLocation.toURI().toURL().toString());

    ResourceStoreRequest validRequest = new ResourceStoreRequest("valid");

    // positive test, valid.txt should be found
    Collection<StorageItem> items = subject.listItems(repository, validRequest);
    assertThat(items.iterator().next().getName(), equalTo("valid.txt"));
    assertThat(items, hasSize(1));


    // missing.txt was listed in this directory, but it does NOT exist, only invalid.txt should be found
    ResourceStoreRequest invalidRequest = new ResourceStoreRequest("invalid");
    items = subject.listItems(repository, invalidRequest);
    assertThat(items.iterator().next().getName(), equalTo("invalid.txt"));
    assertThat(items, hasSize(1));
  }

  /**
   * Expects an already deleted file to thrown an ItemNotFoundException. More specifically if a file was deleted
   * after the call to file.exists() was called.
   */
  @Test(expected = ItemNotFoundException.class)
  public void testRetrieveItemFromFileThrowsItemNotFoundExceptionForDeletedFile()
      throws Exception
  {
    // mock file
    File mockFile = mock(File.class);
    when(mockFile.isDirectory()).thenReturn(false);
    when(mockFile.isFile()).thenReturn(true);
    when(mockFile.exists()).thenReturn(true);


    // needs to throw a FileNotFound when _opening_ the file
    when(linkPersister.isLinkContent(Mockito.any(ContentLocator.class)))
        .thenThrow(new FileNotFoundException("Expected to be thrown from mock."));

    // expected to throw a ItemNotFoundException
    subject.retrieveItemFromFile(repository, new ResourceStoreRequest("not-used"), mockFile);
  }

  /**
   * Verifies that attribute files (used by {@link DefaultFSLocalRepositoryStorage}, that stores attributea within LS)
   * are not checked for being a link, as this is redundant check.
   */
  @Test
  public void attributeFileIsNotCheckedForBeingLink()
      throws Exception
  {
    // mock file
    File mockFile = mock(File.class);
    when(mockFile.isDirectory()).thenReturn(false);
    when(mockFile.isFile()).thenReturn(true);
    when(mockFile.exists()).thenReturn(true);

    // plain file, it result in 1 method call on link persister to check is content a link or not
    when(repositoryItemUid.getBooleanAttributeValue(IsItemAttributeMetacontentAttribute.class)).thenReturn(false);
    subject.retrieveItemFromFile(repository, new ResourceStoreRequest("not-used"), mockFile);
    Mockito.verify(linkPersister, times(1)).isLinkContent(Mockito.any(ContentLocator.class));

    // reset the mock
    Mockito.reset(linkPersister);

    // attribute file, it result in 0 method call on link persister as is redundtant
    when(repositoryItemUid.getBooleanAttributeValue(IsItemAttributeMetacontentAttribute.class)).thenReturn(true);
    subject.retrieveItemFromFile(repository, new ResourceStoreRequest("not-used"), mockFile);
    Mockito.verify(linkPersister, times(0)).isLinkContent(Mockito.any(ContentLocator.class));
  }

  @Test
  public void getFileFromBaseOk() throws Exception {
    final File fileFromBase = subject.getFileFromBase(repository, new ResourceStoreRequest("/foo/bar"));
    assertThat(fileFromBase.getCanonicalFile(), equalTo(new File(baseDir, "foo/bar")));
  }

  @Test
  public void getFileFromBaseRelative() throws Exception {
    {
      final File fileFromBase = subject.getFileFromBase(repository, new ResourceStoreRequest("/foo/bar/../baz"));
      assertThat(fileFromBase.getCanonicalFile(), equalTo(new File(baseDir, "foo/baz")));
    }
    {
      final File fileFromBase = subject.getFileFromBase(repository, new ResourceStoreRequest("/foo/bar/../bar/../../baz"));
      assertThat(fileFromBase.getCanonicalFile(), equalTo(new File(baseDir, "baz")));
    }
  }

  @Test(expected = LocalStorageException.class)
  public void getFileFromBaseRelativeOut1() throws Exception {
    subject.getFileFromBase(repository, new ResourceStoreRequest("/foo/bar/../bar/../../../baz"));
  }

  @Test(expected = LocalStorageException.class)
  public void getFileFromBaseRelativeOut2() throws Exception {
    subject.getFileFromBase(repository, new ResourceStoreRequest(".."));
  }
}
