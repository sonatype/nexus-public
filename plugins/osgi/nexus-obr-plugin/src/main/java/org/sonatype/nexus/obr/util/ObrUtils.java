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
package org.sonatype.nexus.obr.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.obr.metadata.ManagedObrSite;
import org.sonatype.nexus.obr.metadata.ObrMetadataSource;
import org.sonatype.nexus.obr.metadata.ObrResourceReader;
import org.sonatype.nexus.obr.metadata.ObrResourceWriter;
import org.sonatype.nexus.obr.shadow.ObrShadowRepository;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.local.LocalRepositoryStorage;
import org.sonatype.nexus.proxy.walker.AbstractFileWalkerProcessor;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.Walker;
import org.sonatype.nexus.proxy.walker.WalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.osgi.impl.bundle.obr.resource.ResourceImpl.UrlTransformer;
import org.osgi.service.obr.Resource;

/**
 * Various utility methods specific to handling OBRs.
 */
public final class ObrUtils
{
  /**
   * Standard location of general metadata inside a Nexus repository.
   */
  private static final String META_PATH = "/.meta";

  /**
   * Standard location of OBR metadata inside a Nexus repository.
   */
  private static final String OBR_PATH = META_PATH + "/obr.xml";

  /**
   * Ignore files with paths like "/.foo" as well as known text extensions.
   */
  private static final Pattern IGNORE_ITEM_PATH_PATTERN =
      Pattern.compile("(.*?/\\.[^/.].*)|(.*?\\.(txt|pom|xml|sha1|md5|asc))");

  /**
   * Separate OBR URL into a hosting site and a path to the OBR metadata.
   */
  private static final Pattern OBR_SITE_AND_PATH_PATTERN =
      Pattern.compile("(.*?)([^/]+((\\.(xml|zip|obr))|([?&=][^/]*)+))");

  private ObrUtils() {
    // utility class, no instances
  }

  /**
   * Tests whether we should consider this item as an OSGi bundle.
   *
   * @param item the item
   * @return true if it might be an OSGi bundle, otherwise false
   */
  public static boolean acceptItem(final StorageItem item) {
    if (null == item || item instanceof StorageCollectionItem) {
      return false; // ignore directories
    }

    final RepositoryItemUid uid = item.getRepositoryItemUid();

    if (IGNORE_ITEM_PATH_PATTERN.matcher(uid.getPath()).matches()) {
      return false; // ignore text files
    }

    if (uid.getRepository() instanceof ObrShadowRepository) {
      return false; // ignore shadowed items as we already know about them
    }

    return true;
  }

  /**
   * Tests whether the given resource request is for OBR metadata.
   *
   * @param request the resource request
   * @return true if this request is for OBR metadata, otherwise false
   */
  public static boolean isObrMetadataRequest(final ResourceStoreRequest request) {
    return OBR_PATH.equals(request.getRequestPath());
  }

  /**
   * Creates a new UID that points to the OBR metadata for the given repository.
   *
   * @param repository the Nexus repository
   * @return a new UID pointing to the OBR metadata
   */
  public static RepositoryItemUid createObrUid(final Repository repository) {
    return repository.createUid(OBR_PATH);
  }

  public static String[] splitObrSiteAndPath(final String url, final boolean useDefaultIfNotSet) {
    // is this a Nexus managed OBR?
    final int i = url.lastIndexOf(OBR_PATH);
    if (i >= 0) {
      return new String[]{url.substring(0, i + 1), OBR_PATH};
    }

    // don't bother testing URLs that point to local directories
    if (!url.startsWith("file:") || !new File(url).isDirectory()) {
      // attempt to find the split between site and metadata path
      final Matcher matcher = OBR_SITE_AND_PATH_PATTERN.matcher(url);
      if (matcher.matches()) {
        return new String[]{matcher.group(1), '/' + matcher.group(2)};
      }
    }

    if (useDefaultIfNotSet) {
      // assume OBR metadata is in repository.xml
      return new String[]{url, "/repository.xml"};
    }
    else {
      return new String[]{url, null};
    }
  }

  /**
   * Retrieves the OBR metadata from the given containing Nexus repository.
   *
   * @param repository the Nexus repository
   * @return the file item containing OBR metadata
   */
  public static StorageFileItem retrieveObrItem(final Repository repository)
      throws StorageException
  {
    final ResourceStoreRequest request = new ResourceStoreRequest(OBR_PATH);

    try {
      // caller is already trusted at this point, so side-step security
      @SuppressWarnings("deprecation")
      final StorageItem item = repository.retrieveItem(false, request);
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
    }
    catch (final ItemNotFoundException e) {
      // OBR metadata is missing, so drop through and provide blank repository
    }
    catch (final IllegalOperationException e) {
      throw new StorageException(e);
    }

    return new DefaultStorageFileItem(repository, request, true, true, new StringContentLocator("<repository/>"));
  }

  /**
   * Retrieves the given item from its local repository cache.
   *
   * @param uid the item UID
   * @return the file item, null if there was a problem retrieving it
   */
  public static StorageFileItem getCachedItem(final RepositoryItemUid uid) {
    final Repository repository = uid.getRepository();

    try {
      final ResourceStoreRequest request = new ResourceStoreRequest(uid.getPath());
      final StorageItem item = repository.getLocalStorage().retrieveItem(repository, request);
      if (item instanceof StorageFileItem) {
        return (StorageFileItem) item;
      }
    }
    catch (final ItemNotFoundException e) {
      // drop through
    }
    catch (final StorageException e) {
      // drop through
    }

    return null;
  }

  /**
   * Calculates the number of "../" segments needed to reach the root from the given path.
   *
   * @param path the starting path
   * @return the relative path to root
   */
  public static String getPathToRoot(final String path) {
    final String normalizedPath = normalize(StringUtils.stripStart(path, "/"));
    return StringUtils.repeat("../", StringUtils.countMatches(normalizedPath, "/"));
  }

  /**
   * Creates a new {@link UrlTransformer} that makes resource URLs relative to the OBR metadata path.
   *
   * @param rootUrl      the root URL
   * @param metadataPath the metadata path
   * @return a relative {@link UrlTransformer}
   */
  public static UrlTransformer getUrlChomper(final URL rootUrl, final String metadataPath) {
    final String rootUrlPattern = '^' + Pattern.quote(rootUrl.toExternalForm()) + "/*";
    final String pathFromMetadataToRoot = getPathToRoot(metadataPath);

    return new UrlTransformer()
    {
      public String transform(final URL url) {
        return url.toExternalForm().replaceFirst(rootUrlPattern, pathFromMetadataToRoot);
      }
    };

  }

  /**
   * Builds OBR metadata by walking the target repository and processing any potential OSGi bundle resources.
   *
   * @param source the OBR metadata source
   * @param uid    the metadata UID
   * @param target the target repository
   * @param walker the repository walker
   */
  public static void buildObr(final ObrMetadataSource source, final RepositoryItemUid uid, final Repository target,
                              final Walker walker)
      throws StorageException
  {
    final ObrResourceWriter writer = source.getWriter(uid);

    try {
      final AbstractFileWalkerProcessor obrProcessor = new AbstractFileWalkerProcessor()
      {
        @Override
        protected void processFileItem(final WalkerContext context, final StorageFileItem item)
            throws IOException
        {
          final Resource resource = source.buildResource(item);
          if (null != resource) {
            writer.append(resource);
          }
        }
      };

      final ResourceStoreRequest request = new ResourceStoreRequest("/", true, false);
      final DefaultWalkerContext ctx = new DefaultWalkerContext(target, request, new ObrWalkerFilter());
      ctx.getProcessors().add(obrProcessor);
      walker.walk(ctx);

      writer.complete(); // the OBR is only updated once the stream is complete and closed
    }
    catch (final WalkerException e) {
      writer.complete();
    }
    finally {
      IOUtils.closeQuietly(writer);
    }
  }

  /**
   * Updates the OBR metadata by streaming the resources and adding/updating/removing the affected resource.
   *
   * @param source   the OBR metadata source
   * @param uid      the metadata UID
   * @param resource the affected resource
   * @param adding   true when adding/updating, false when removing
   */
  public static void updateObr(final ObrMetadataSource source, final RepositoryItemUid uid, final Resource resource,
                               boolean adding)
      throws StorageException
  {
    ObrResourceWriter writer = null;
    ObrResourceReader reader = null;

    try {
      writer = source.getWriter(uid);
      reader = source.getReader(new ManagedObrSite(retrieveObrItem(uid.getRepository())));
      for (Resource i = reader.readResource(); i != null; i = reader.readResource()) {
        if (i.equals(resource)) {
          if (adding) // only update once, remove any duplicates
          {
            writer.append(resource);
            adding = false;
          }
        }
        else {
          writer.append(i);
        }
      }

      if (adding) // not seen this resource before
      {
        writer.append(resource);
      }

      writer.complete(); // the OBR is only updated once the stream is complete and closed
    }
    catch (final IOException e) {
      throw new StorageException(e);
    }
    finally {
      // avoid file locks by closing reader first
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(writer);
    }
  }

  /**
   * Add any relevant virtual OBR items to the given directory listing.
   *
   * @param uid   the directory item UID
   * @param items the original list of items
   * @return augmented list of items
   */
  public static Collection<StorageItem> augmentListedItems(final RepositoryItemUid uid,
                                                           final Collection<StorageItem> items)
  {
    final Repository repository = uid.getRepository();
    ResourceStoreRequest request;

    final Collection<StorageItem> augmentedItems = new ArrayList<StorageItem>(items);
    final LocalRepositoryStorage storage = repository.getLocalStorage();

    try {
      if ("/".equals(uid.getPath())) {
        request = new ResourceStoreRequest(META_PATH);
        if (!storage.containsItem(repository, request)) {
          // need to create /.meta so we can safely traverse into it later on...
          final StorageItem metaDir = new DefaultStorageCollectionItem(repository, request, true, true);
          storage.storeItem(repository, metaDir);
          augmentedItems.add(metaDir);
        }
      }
      else if (META_PATH.equals(uid.getPath())) {
        request = new ResourceStoreRequest(OBR_PATH);
        if (!storage.containsItem(repository, request)) {
          // add a temporary storage item to the list (don't actually store it)
          final ContentLocator content = new StringContentLocator("<repository/>");
          final StorageItem obrFile = new DefaultStorageFileItem(repository, request, true, true, content);
          augmentedItems.add(obrFile);
        }
      }
    }
    catch (final Exception e) {
      // ignore
    }

    return augmentedItems;
  }

  /**
   * Normalize a path.
   * Eliminates "/../" and "/./" in a string. Returns <code>null</code> if the ..'s went past the
   * root.
   * Eg:
   * <pre>
   * /foo//               -->     /foo/
   * /foo/./              -->     /foo/
   * /foo/../bar          -->     /bar
   * /foo/../bar/         -->     /bar/
   * /foo/../bar/../baz   -->     /baz
   * //foo//./bar         -->     /foo/bar
   * /../                 -->     null
   * </pre>
   *
   * @param path the path to normalize
   * @return the normalized String, or <code>null</code> if too many ..'s.
   */
  private static String normalize(final String path) {
    String normalized = path;
    // Resolve occurrences of "//" in the normalized path
    while (true) {
      int index = normalized.indexOf("//");
      if (index < 0) {
        break;
      }
      normalized = normalized.substring(0, index) + normalized.substring(index + 1);
    }

    // Resolve occurrences of "/./" in the normalized path
    while (true) {
      int index = normalized.indexOf("/./");
      if (index < 0) {
        break;
      }
      normalized = normalized.substring(0, index) + normalized.substring(index + 2);
    }

    // Resolve occurrences of "/../" in the normalized path
    while (true) {
      int index = normalized.indexOf("/../");
      if (index < 0) {
        break;
      }
      if (index == 0) {
        return null;  // Trying to go outside our context
      }
      int index2 = normalized.lastIndexOf('/', index - 1);
      normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
    }

    // Return the normalized path that we have completed
    return normalized;
  }


}
