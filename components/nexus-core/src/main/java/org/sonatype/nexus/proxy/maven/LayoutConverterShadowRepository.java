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
package org.sonatype.nexus.proxy.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.NoSuchResourceStoreException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.nexus.proxy.repository.AbstractShadowRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.IncompatibleMasterRepositoryException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;

import com.google.common.collect.Lists;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

/**
 * Base class for shadows that make "gateways" from M1 to M2 lauouts and vice versa.
 *
 * @author cstamas
 */
public abstract class LayoutConverterShadowRepository
    extends AbstractShadowRepository
    implements MavenShadowRepository
{
  /**
   * The GAV Calculator.
   */
  private GavCalculator m1GavCalculator;

  /**
   * The GAV Calculator.
   */
  private  GavCalculator m2GavCalculator;

  /**
   * Metadata manager.
   */
  private MetadataManager metadataManager;

  /**
   * The artifact packaging mapper.
   */
  private ArtifactPackagingMapper artifactPackagingMapper;

  /**
   * Repository kind.
   */
  private RepositoryKind repositoryKind;

  /**
   * ArtifactStoreHelper.
   */
  private ArtifactStoreHelper artifactStoreHelper;
  
  @Inject
  public void populateLayoutConverterShadowRepository(final @Named("maven1") GavCalculator m1GavCalculator,
      final @Named("maven2") GavCalculator m2GavCalculator, final MetadataManager metadataManager,
      final ArtifactPackagingMapper artifactPackagingMapper)
  {
    this.m1GavCalculator = checkNotNull(m1GavCalculator);
    this.m2GavCalculator = checkNotNull(m2GavCalculator);
    this.metadataManager = checkNotNull(metadataManager);
    this.artifactPackagingMapper = checkNotNull(artifactPackagingMapper);
    this.repositoryKind = new DefaultRepositoryKind(MavenShadowRepository.class,
        Arrays.asList(new Class<?>[] { MavenRepository.class }));
    this.artifactStoreHelper = new ArtifactStoreHelper(this);
  }

  @Override
  public RepositoryKind getRepositoryKind() {
    return repositoryKind;
  }

  @Override
  public MavenRepository getMasterRepository() {
    return super.getMasterRepository().adaptToFacet(MavenRepository.class);
  }

  @Override
  public void setMasterRepository(Repository masterRepository)
      throws IncompatibleMasterRepositoryException
  {
    // we allow only MavenRepository instances as masters
    if (!masterRepository.getRepositoryKind().isFacetAvailable(MavenRepository.class)) {
      throw new IncompatibleMasterRepositoryException(
          "This shadow repository needs master repository which implements MavenRepository interface!", this,
          masterRepository.getId());
    }

    super.setMasterRepository(masterRepository);
  }

  public GavCalculator getM1GavCalculator() {
    return m1GavCalculator;
  }

  public GavCalculator getM2GavCalculator() {
    return m2GavCalculator;
  }

  @Override
  public ArtifactPackagingMapper getArtifactPackagingMapper() {
    return artifactPackagingMapper;
  }

  @Override
  public RepositoryPolicy getRepositoryPolicy() {
    return getMasterRepository().getRepositoryPolicy();
  }

  @Override
  public void setRepositoryPolicy(RepositoryPolicy repositoryPolicy) {
    throw new UnsupportedOperationException("This method is not supported on Repository of type SHADOW");
  }

  @Override
  public boolean isMavenArtifact(StorageItem item) {
    return isMavenArtifactPath(item.getPath());
  }

  @Override
  public boolean isMavenMetadata(StorageItem item) {
    return isMavenMetadataPath(item.getPath());
  }

  @Override
  public boolean isMavenArtifactPath(String path) {
    return getGavCalculator().pathToGav(path) != null;
  }

  @Override
  public abstract boolean isMavenMetadataPath(String path);

  @Override
  public MetadataManager getMetadataManager() {
    return metadataManager;
  }

  @Override
  public boolean recreateMavenMetadata(final ResourceStoreRequest request) {
    return false;
  }

  @Override
  public void storeItemWithChecksums(final ResourceStoreRequest request, final InputStream is,
                                     final Map<String, String> userAttributes)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
  {
    String originalPath = request.getRequestPath();

    if (log.isDebugEnabled()) {
      log.debug("storeItemWithChecksums() :: " + request.getRequestPath());
    }

    try {
      try {
        storeItem(request, is, userAttributes);
      }
      catch (IOException e) {
        throw new LocalStorageException("Could not get the content from the ContentLocator!", e);
      }

      StorageFileItem storedFile = (StorageFileItem) retrieveItem(false, request);

      String sha1Hash = storedFile.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);

      String md5Hash = storedFile.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

      if (!StringUtils.isEmpty(sha1Hash)) {
        request.setRequestPath(storedFile.getPath() + ".sha1");

        storeItem(false, new DefaultStorageFileItem(this, request, true, true, new StringContentLocator(
            sha1Hash)));
      }

      if (!StringUtils.isEmpty(md5Hash)) {
        request.setRequestPath(storedFile.getPath() + ".md5");

        storeItem(false, new DefaultStorageFileItem(this, request, true, true, new StringContentLocator(
            md5Hash)));
      }
    }
    catch (ItemNotFoundException e) {
      throw new LocalStorageException("Storage inconsistency!", e);
    }
    finally {
      request.setRequestPath(originalPath);
    }
  }

  @Override
  public void deleteItemWithChecksums(final ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
             StorageException, AccessDeniedException
  {
    if (log.isDebugEnabled()) {
      log.debug("deleteItemWithChecksums() :: " + request.getRequestPath());
    }

    try {
      deleteItem(request);
    }
    catch (ItemNotFoundException e) {
      if (request.getRequestPath().endsWith(".asc")) {
        // Do nothing no guarantee that the .asc files will exist
      }
      else {
        throw e;
      }
    }

    String originalPath = request.getRequestPath();

    request.setRequestPath(originalPath + ".sha1");

    try {
      deleteItem(request);
    }
    catch (ItemNotFoundException e) {
      // ignore not found
    }

    request.setRequestPath(originalPath + ".md5");

    try {
      deleteItem(request);
    }
    catch (ItemNotFoundException e) {
      // ignore not found
    }

    // Now remove the .asc files, and the checksums stored with them as well
    // Note this is a recursive call, hence the check for .asc
    if (!originalPath.endsWith(".asc")) {
      request.setRequestPath(originalPath + ".asc");

      deleteItemWithChecksums(request);
    }
  }

  @Override
  public void storeItemWithChecksums(final boolean fromTask, final AbstractStorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("storeItemWithChecksums() :: " + item.getRepositoryItemUid().toString());
    }

    try {
      try {
        storeItem(fromTask, item);
      }
      catch (IOException e) {
        throw new LocalStorageException("Could not get the content from the ContentLocator!", e);
      }

      StorageFileItem storedFile = (StorageFileItem) retrieveItem(fromTask, new ResourceStoreRequest(item));

      ResourceStoreRequest req = new ResourceStoreRequest(storedFile);

      String sha1Hash = storedFile.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_SHA1_KEY);

      String md5Hash = storedFile.getRepositoryItemAttributes().get(DigestCalculatingInspector.DIGEST_MD5_KEY);

      if (!StringUtils.isEmpty(sha1Hash)) {
        req.setRequestPath(item.getPath() + ".sha1");

        storeItem(fromTask, new DefaultStorageFileItem(this, req, true, true, new StringContentLocator(
            sha1Hash)));
      }

      if (!StringUtils.isEmpty(md5Hash)) {
        req.setRequestPath(item.getPath() + ".md5");

        storeItem(fromTask, new DefaultStorageFileItem(this, req, true, true, new StringContentLocator(
            md5Hash)));
      }
    }
    catch (ItemNotFoundException e) {
      throw new LocalStorageException("Storage inconsistency!", e);
    }
  }

  @Override
  public void deleteItemWithChecksums(final boolean fromTask, final ResourceStoreRequest request)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (log.isDebugEnabled()) {
      log.debug("deleteItemWithChecksums() :: " + request.toString());
    }

    deleteItem(fromTask, request);

    request.pushRequestPath(request.getRequestPath() + ".sha1");
    try {
      deleteItem(fromTask, request);
    }
    catch (ItemNotFoundException e) {
      // ignore not found
    }
    finally {
      request.popRequestPath();
    }

    request.pushRequestPath(request.getRequestPath() + ".md5");
    try {
      deleteItem(fromTask, request);
    }
    catch (ItemNotFoundException e) {
      // ignore not found
    }
    finally {
      request.popRequestPath();
    }
  }

  @Override
  public ArtifactStoreHelper getArtifactStoreHelper() {
    return artifactStoreHelper;
  }

  // =================================================================================
  // ShadowRepository customizations

  /**
   * Transforms a full artifact path from M1 layout to M2 layout.
   */
  protected List<String> transformM1toM2(final String path) {
    final Gav gav = getM1GavCalculator().pathToGav(path);

    // Unsupported path
    if (gav == null) {
      return null;
    }
    // m2 repo is layouted as:
    // g/i/d
    // aid
    // version
    // files

    StringBuilder sb = new StringBuilder(RepositoryItemUid.PATH_ROOT);
    sb.append(gav.getGroupId().replaceAll("\\.", "/"));
    sb.append(RepositoryItemUid.PATH_SEPARATOR);
    sb.append(gav.getArtifactId());
    sb.append(RepositoryItemUid.PATH_SEPARATOR);
    sb.append(gav.getVersion());
    sb.append(RepositoryItemUid.PATH_SEPARATOR);
    sb.append(gav.getName());
    return Collections.singletonList(sb.toString());
  }

  /**
   * Transforms a full artifact path from M2 layout to M1 layout.
   */
  protected List<String> transformM2toM1(final String path,
                                         final List<String> extraFolders)
  {
    final Gav gav = getM2GavCalculator().pathToGav(path);

    // Unsupported path
    if (gav == null) {
      return null;
    }

    final List<String> exts = Lists.newArrayList();
    exts.add(gav.getExtension() + "s");
    if ((extraFolders != null) && !extraFolders.isEmpty()) {
      exts.addAll(extraFolders);
    }
    final List<String> result = Lists.newArrayListWithCapacity(exts.size());
    for (final String ext : exts) {
      // m1 repo is layouted as:
      // g.i.d
      // poms/jars/java-sources/licenses
      // files
      final StringBuilder sb = new StringBuilder(RepositoryItemUid.PATH_ROOT);
      sb.append(gav.getGroupId());
      sb.append(RepositoryItemUid.PATH_SEPARATOR);
      sb.append(ext);
      sb.append(RepositoryItemUid.PATH_SEPARATOR);

      // NEXUS-6185: Fix for M2 timestamped snapshots
      if (gav.isSnapshot()) {
        // we're dealing with files like
        // artifact-1.2-20131207.174838-3.jar
        // goal is
        // artifact-1.2-SNAPSHOT
        sb.append(gav.getArtifactId()).append("-").append(gav.getBaseVersion());

        if (!StringUtils.isEmpty(gav.getClassifier())) {
          sb.append("-").append(gav.getClassifier());
        }
        sb.append(".").append(gav.getExtension());
        if (gav.isHash()) {
          sb.append(".").append(gav.getHashType());
        }
        if (gav.isSignature()) {
          sb.append(".").append(gav.getSignatureType());
        }
      }
      else {
        sb.append(gav.getName());
      }
      result.add(sb.toString());
    }
    return result;
  }

  @Override
  protected StorageLinkItem createLink(final StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
  {
    List<String> shadowPaths = transformMaster2Shadow(item.getPath());

    if (shadowPaths != null && !shadowPaths.isEmpty()) {
      ResourceStoreRequest req = new ResourceStoreRequest(shadowPaths.get(0));
      req.getRequestContext().setParentContext(item.getItemContext());

      DefaultStorageLinkItem link =
          new DefaultStorageLinkItem(this, req, true, true, item.getRepositoryItemUid());

      storeItem(false, link);

      return link;
    }
    else {
      return null;
    }
  }

  @Override
  protected void deleteLink(final StorageItem item)
      throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
  {
    List<String> shadowPaths = transformMaster2Shadow(item.getPath());

    if (shadowPaths != null && !shadowPaths.isEmpty()) {
      ResourceStoreRequest req = new ResourceStoreRequest(shadowPaths.get(0));
      req.getRequestContext().setParentContext(item.getItemContext());
      try {
        deleteItem(false, req);
      }
      catch (ItemNotFoundException e) {
        // NEXUS-5673: just ignore it silently, this might happen when
        // link to be deleted was not found in shadow (like a parent folder was deleted
        // or M2 checksum file in master)
        // simply ignoring this is okay, as our initial goal is to lessen log spam.
        // If parent cleanup fails below, thats fine too, superclass with handle the
        // exception. This catch here is merely to logic continue with empty parent cleanup
      }

      // we need to clean up empty shadow parent directories
      String parentPath =
          req.getRequestPath().substring(0, req.getRequestPath().lastIndexOf(item.getName()));
      ResourceStoreRequest parentRequest = new ResourceStoreRequest(parentPath);

      while (parentRequest != null) {
        StorageItem parentItem = null;
        parentItem = this.retrieveItem(false, parentRequest);

        // this should be a collection Item
        if (StorageCollectionItem.class.isInstance(parentItem)) {
          StorageCollectionItem parentCollectionItem = (StorageCollectionItem) parentItem;
          try {
            if (parentCollectionItem.list().size() == 0) {
              deleteItem(false, parentRequest);
              parentRequest = new ResourceStoreRequest(parentCollectionItem.getParentPath());
            }
            else {
              // exit loop
              parentRequest = null;
            }
          }
          catch (AccessDeniedException e) {
            this.log.debug(
                "Failed to delete shadow parent: " + this.getId() + ":" + parentItem.getPath()
                    + " Access Denied", e);
            // exit loop
            parentRequest = null;
          }
          catch (NoSuchResourceStoreException e) {
            this.log.debug(
                "Failed to delete shadow parent: " + this.getId() + ":" + parentItem.getPath()
                    + " does not exist", e);
            // exit loop
            parentRequest = null;
          }
        }
        else {
          this.log.debug("ExpectedCollectionItem, found: " + parentItem.getClass() + ", ignoring.");
        }
      }
    }
  }

  /**
   * Gets the shadow path from master path. If path is not transformable, return null.
   *
   * @param path the path
   * @return the shadow path
   */
  protected abstract List<String> transformMaster2Shadow(String path);

  @Override
  protected StorageItem doRetrieveItem(final ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    StorageItem result = null;

    try {
      result = super.doRetrieveItem(request);

      return result;
    }
    catch (ItemNotFoundException e) {
      // if it is thrown by super.doRetrieveItem()
      List<String> transformedPaths = transformShadow2Master(request.getRequestPath());

      if (transformedPaths == null || transformedPaths.isEmpty()) {
        throw new ItemNotFoundException(reasonFor(request, this,
            "Request path %s is not transformable to master.", request.getRequestPath()));
      }

      for (String transformedPath : transformedPaths) {
        // delegate the call to the master
        request.pushRequestPath(transformedPath);
        try {
          result = doRetrieveItemFromMaster(request);

          // try to create link on the fly
          try {
            StorageLinkItem link = createLink(result);

            if (link != null) {
              return link;
            }
            else {
              // fallback to result, but will not happen, see above
              return result;
            }
          }
          catch (Exception e1) {
            // fallback to result, but will not happen, see above
            return result;
          }
        }
        catch (ItemNotFoundException ex) {
          // neglect, we might try another transformed path
        }
        finally {
          request.popRequestPath();
        }
      }

      throw new ItemNotFoundException(request, this);
    }
  }

  /**
   * Gets the master path from shadow path. If path is not transformable, return null.
   *
   * @param path the path
   * @return the master path
   */
  protected abstract List<String> transformShadow2Master(String path);
}
