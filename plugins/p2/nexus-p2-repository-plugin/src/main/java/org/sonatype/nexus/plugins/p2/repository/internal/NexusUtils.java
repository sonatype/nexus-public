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
package org.sonatype.nexus.plugins.p2.repository.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.fs.DefaultFSLocalRepositoryStorage;
import org.sonatype.nexus.util.file.DirSupport;

public class NexusUtils
{

  private static final String GENERATED_AT_ATTRIBUTE = "p2Repository.generated.timestamp";

  private static final String DOT = ".";

  private NexusUtils() {
  }

  static File retrieveFile(final Repository repository, final String path)
      throws LocalStorageException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(path);
    final File content =
        ((DefaultFSLocalRepositoryStorage) repository.getLocalStorage()).getFileFromBase(repository, request);
    return content;
  }

  static File safeRetrieveFile(final Repository repository, final String path) {
    try {
      return retrieveFile(repository, path);
    }
    catch (final LocalStorageException e) {
      return null;
    }
  }

  static StorageItem retrieveItem(final Repository repository, final String path)
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(path);
    final StorageItem item = repository.retrieveItem(request);
    return item;
  }

  static StorageItem safeRetrieveItem(final Repository repository, final String path) {
    try {
      return retrieveItem(repository, path);
    }
    catch (final Exception e) {
      return null;
    }
  }

  public static void storeItem(final Repository repository, final ResourceStoreRequest request, final InputStream in,
                               final String mimeType, final Map<String, String> userAttributes)
      throws Exception
  {
    final DefaultStorageFileItem fItem =
        new DefaultStorageFileItem(repository, request, true, true,
            new PreparedContentLocator(in, mimeType, ContentLocator.UNKNOWN_LENGTH));

    if (userAttributes != null) {
      fItem.getRepositoryItemAttributes().putAll(userAttributes);
    }

    repository.storeItem(false, fItem);
  }

  static void createLink(final Repository repository, final StorageItem item, final String path)
      throws Exception
  {
    final ResourceStoreRequest req = new ResourceStoreRequest(path);

    req.getRequestContext().setParentContext(item.getItemContext());

    final DefaultStorageLinkItem link =
        new DefaultStorageLinkItem(repository, req, true, true, item.getRepositoryItemUid());

    repository.storeItem(false, link);
  }

  static File localStorageOfRepositoryAsFile(final Repository repository)
      throws LocalStorageException
  {
    if (repository.getLocalUrl() != null
        && repository.getLocalStorage() instanceof DefaultFSLocalRepositoryStorage) {
      final File baseDir =
          ((DefaultFSLocalRepositoryStorage) repository.getLocalStorage()).getBaseDir(repository,
              new ResourceStoreRequest(
                  RepositoryItemUid.PATH_ROOT));
      return baseDir;
    }

    throw new LocalStorageException(String.format("Repository [%s] does not have an local storage",
        repository.getId()));
  }

  static String getRelativePath(final File fromFile, final File toFile) {
    final String[] fromSegments = getReversePathSegments(fromFile);
    final String[] toSegments = getReversePathSegments(toFile);

    String relativePath = "";
    int i = fromSegments.length - 1;
    int j = toSegments.length - 1;

    // first eliminate common root
    while ((i >= 0) && (j >= 0) && (fromSegments[i].equals(toSegments[j]))) {
      i--;
      j--;
    }

    for (; i >= 0; i--) {
      relativePath += ".." + File.separator;
    }

    for (; j >= 1; j--) {
      relativePath += toSegments[j] + File.separator;
    }

    relativePath += toSegments[j];

    return relativePath;
  }

  static boolean isHidden(final String path) {
    if (path == null) {
      return false;
    }
    return path.startsWith(DOT) || path.startsWith("/" + DOT) || path.startsWith(File.separator + DOT);
  }

  public static File createTemporaryP2Repository()
      throws IOException
  {
    File tempP2Repository;
    tempP2Repository = File.createTempFile("nexus-p2-repository-plugin", "");
    DirSupport.delete(tempP2Repository.toPath());
    DirSupport.mkdir(tempP2Repository.toPath());
    return tempP2Repository;
  }

  public static void storeItemFromFile(final String path,
                                       final File file,
                                       final Repository repository,
                                       final String mimeType)
      throws Exception
  {
    try (InputStream in = new FileInputStream(file)) {
      final Map<String, String> attributes = new HashMap<String, String>();
      attributes.put(GENERATED_AT_ATTRIBUTE, new Date().toString());

      final ResourceStoreRequest request = new ResourceStoreRequest(path);

      storeItem(repository, request, in, mimeType, attributes);
    }
  }

  private static String[] getReversePathSegments(final File file) {
    final List<String> paths = new ArrayList<String>();

    File segment;
    try {
      segment = file.getCanonicalFile();
      while (segment != null) {
        paths.add(segment.getName());
        segment = segment.getParentFile();
      }
    }
    catch (final IOException e) {
      return null;
    }
    return paths.toArray(new String[paths.size()]);
  }

}