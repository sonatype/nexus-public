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
package org.sonatype.nexus.proxy.maven.maven2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEvent;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageCompositeFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageCompositeFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.AbstractMavenGroupRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.MavenRepositoryMetadataValidationEventFailed;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.NexusMergeOperation;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.utils.RepositoryStringUtils;
import org.sonatype.nexus.util.DigesterUtils;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.proxy.ItemNotFoundException.reasonFor;

@Named(M2GroupRepository.ID)
@Typed(GroupRepository.class)
@Description("Maven2 Repository Group")
public class M2GroupRepository
    extends AbstractMavenGroupRepository
{
  /**
   * This "mimics" the @Named("maven2")
   */
  public static final String ID = Maven2ContentClass.ID;

  private final ContentClass contentClass;

  /**
   * The GAV Calculator.
   */
  private final GavCalculator gavCalculator;

  /**
   * Content class.
   */
  private final M2GroupRepositoryConfigurator m2GroupRepositoryConfigurator;

  @Inject
  public M2GroupRepository(final @Named(Maven2ContentClass.ID) ContentClass contentClass, 
                           final @Named("maven2") GavCalculator gavCalculator,
                           final M2GroupRepositoryConfigurator m2GroupRepositoryConfigurator)
  {
    this.contentClass = checkNotNull(contentClass);
    this.gavCalculator = checkNotNull(gavCalculator);
    this.m2GroupRepositoryConfigurator = checkNotNull(m2GroupRepositoryConfigurator);
  }

  @Override
  protected M2GroupRepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (M2GroupRepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<M2GroupRepositoryConfiguration> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<M2GroupRepositoryConfiguration>()
    {
      public M2GroupRepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new M2GroupRepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
      }
    };
  }

  @Override
  public ContentClass getRepositoryContentClass() {
    return contentClass;
  }

  @Override
  public GavCalculator getGavCalculator() {
    return gavCalculator;
  }

  @Override
  protected Configurator getConfigurator() {
    return m2GroupRepositoryConfigurator;
  }

  @Override
  public boolean isMavenMetadataPath(String path) {
    return M2ArtifactRecognizer.isMetadata(path);
  }

  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    if (M2ArtifactRecognizer.isMetadata(request.getRequestPath())
        && !M2ArtifactRecognizer.isChecksum(request.getRequestPath())) {
      // metadata checksum files are calculated and cached as side-effect
      // of doRetrieveMetadata.

      try {
        return doRetrieveMetadata(request);
      }
      catch (UnsupportedStorageOperationException e) {
        throw new LocalStorageException(e);
      }
    }

    return super.doRetrieveItem(request);
  }

  /**
   * Parse a maven Metadata object from a storage file item
   */
  private Metadata parseMetadata(StorageFileItem fileItem)
      throws IOException, MetadataException
  {
    Metadata metadata;
    try (InputStream inputStream = fileItem.getInputStream()) {
      metadata = MetadataBuilder.read(inputStream);
    }

    MavenRepository repo = fileItem.getRepositoryItemUid().getRepository().adaptToFacet(MavenRepository.class);
    RepositoryPolicy policy = repo.getRepositoryPolicy();
    if (metadata != null && metadata.getVersioning() != null) {
      List<String> versions = metadata.getVersioning().getVersions();
      if (RepositoryPolicy.RELEASE.equals(policy)) {
        metadata.getVersioning().setSnapshot(null);
        String latest = filterMetadata(versions, false);
        metadata.getVersioning().setLatest(latest);
      }
      else if (RepositoryPolicy.SNAPSHOT.equals(policy)) {
        metadata.getVersioning().setRelease(null);
        String latest = filterMetadata(versions, true);
        metadata.getVersioning().setLatest(latest);
      }
    }

    return metadata;
  }

  private String filterMetadata(List<String> versions, boolean allowSnapshot) {
    String latest = null;
    for (Iterator<String> it = versions.iterator(); it.hasNext(); ) {
      String version = it.next();
      if (allowSnapshot ^ Gav.isSnapshot(version)) {
        it.remove();
      }
      else {
        latest = version;
      }
    }
    return latest;
  }

  /**
   * Aggregates metadata from all member repositories
   */
  private StorageItem doRetrieveMetadata(ResourceStoreRequest request)
      throws StorageException, IllegalOperationException, UnsupportedStorageOperationException, ItemNotFoundException
  {
    List<StorageItem> items = doRetrieveItems(request);

    if (items.isEmpty()) {
      throw new ItemNotFoundException(reasonFor(request, this,
          "Metadata %s not found in any of the members of %s.", request.getRequestPath(),
          RepositoryStringUtils.getHumanizedNameString(this)));
    }

    if (!isMergeMetadata()) {
      // not merging: return the 1st and ciao
      return items.get(0);
    }

    List<Metadata> existingMetadatas = new ArrayList<Metadata>();

    try {
      for (StorageItem item : items) {
        if (!(item instanceof StorageFileItem)) {
          break;
        }

        StorageFileItem fileItem = (StorageFileItem) item;

        try {
          existingMetadatas.add(parseMetadata(fileItem));
        }
        catch (IOException e) {
          log.warn(
              "IOException during parse of metadata UID=\"" + fileItem.getRepositoryItemUid().toString()
                  + "\", will be skipped from aggregation!", e);

          eventBus().post(
              newMetadataFailureEvent(fileItem,
                  "Invalid metadata served by repository. If repository is proxy, please check out what is it serving!"));
        }
        catch (MetadataException e) {
          log.warn(
              "Metadata exception during parse of metadata from UID=\""
                  + fileItem.getRepositoryItemUid().toString() + "\", will be skipped from aggregation!", e);

          eventBus().post(
              newMetadataFailureEvent(fileItem,
                  "Invalid metadata served by repository. If repository is proxy, please check out what is it serving!"));
        }
      }

      if (existingMetadatas.isEmpty()) {
        throw new ItemNotFoundException(reasonFor(request, this,
            "Metadata %s not parseable in any of the members of %s.", request.getRequestPath(),
            RepositoryStringUtils.getHumanizedNameString(this)));
      }

      Metadata result = existingMetadatas.get(0);

      // do a merge if necessary
      if (existingMetadatas.size() > 1) {
        List<MetadataOperation> ops = new ArrayList<MetadataOperation>();

        for (int i = 1; i < existingMetadatas.size(); i++) {
          ops.add(new NexusMergeOperation(new MetadataOperand(existingMetadatas.get(i))));
        }

        final Collection<MetadataException> metadataExceptions =
            MetadataBuilder.changeMetadataIgnoringFailures(result, ops);
        if (metadataExceptions != null && !metadataExceptions.isEmpty()) {
          for (final MetadataException metadataException : metadataExceptions) {
            log.warn(
                "Ignored exception during M2 metadata merging: " + metadataException.getMessage()
                    + " (request " + request.getRequestPath() + ")", metadataException);
          }
        }
      }

      // build the result item
      ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();

      MetadataBuilder.write(result, resultOutputStream);

      StorageItem item = createMergedMetadataItem(request, resultOutputStream.toByteArray(), items);

      // build checksum files
      String md5Digest = DigesterUtils.getMd5Digest(resultOutputStream.toByteArray());

      String sha1Digest = DigesterUtils.getSha1Digest(resultOutputStream.toByteArray());

      String sha256Digest = DigesterUtils.getSha256Digest(resultOutputStream.toByteArray());

      String sha512Digest = DigesterUtils.getSha512Digest(resultOutputStream.toByteArray());

      storeMergedMetadataItemDigest(request, md5Digest, items, "MD5");

      storeMergedMetadataItemDigest(request, sha1Digest, items, "SHA1");

      storeMergedMetadataItemDigest(request, sha256Digest, items, "SHA-256");

      storeMergedMetadataItemDigest(request, sha512Digest, items, "SHA-512");

      resultOutputStream.close();

      if (log.isDebugEnabled()) {
        log.debug(
            "Item for path " + request.toString() + " merged from " + Integer.toString(items.size())
                + " found items.");
      }

      return item;

    }
    catch (IOException e) {
      throw new LocalStorageException("Got IOException during M2 metadata merging.", e);
    }
    catch (MetadataException e) {
      throw new LocalStorageException("Got MetadataException during M2 metadata merging.", e);
    }
  }

  protected void storeMergedMetadataItemDigest(ResourceStoreRequest request, String digest,
                                               List<StorageItem> sources, String algorithm)
      throws IOException, UnsupportedStorageOperationException, IllegalOperationException
  {
    String digestFileName = request.getRequestPath() + "." + algorithm.toLowerCase();

    String mimeType = getMimeSupport().guessMimeTypeFromPath(getMimeRulesSource(), digestFileName);

    byte[] bytes = (digest + '\n').getBytes("UTF-8");

    ContentLocator contentLocator = new ByteArrayContentLocator(bytes, mimeType);

    ResourceStoreRequest req = new ResourceStoreRequest(digestFileName);

    req.getRequestContext().setParentContext(request.getRequestContext());
    req.getRequestContext().setRequestIsExternal(false); // override in THIS context possible TRUE from parent

    // Metadata checksum files are not composite ones, they are derivatives of the Metadata (and metadata file _is_
    // composite one)
    DefaultStorageFileItem digestFileItem = new DefaultStorageFileItem(this, req, true, false, contentLocator);

    storeItem(false, digestFileItem);
  }

  protected StorageCompositeFileItem createMergedMetadataItem(ResourceStoreRequest request, byte[] content,
                                                              List<StorageItem> sources)
  {
    // we are creating file maven-metadata.xml, and ask the MimeUtil for it's exact MIME type to honor potential
    // user configuration
    String mimeType = getMimeSupport().guessMimeTypeFromPath(getMimeRulesSource(), "maven-metadata.xml");

    ContentLocator contentLocator = new ByteArrayContentLocator(content, mimeType);

    DefaultStorageCompositeFileItem result =
        new DefaultStorageCompositeFileItem(this, request, true, false, contentLocator, sources);

    result.setCreated(getNewestCreatedDate(sources));

    result.setModified(result.getCreated());

    return result;
  }

  private long getNewestCreatedDate(List<StorageItem> sources) {
    long result = 0;

    for (StorageItem source : sources) {
      result = Math.max(result, source.getCreated());
    }

    return result;
  }

  private RepositoryItemValidationEvent newMetadataFailureEvent(StorageFileItem item, String msg) {
    return new MavenRepositoryMetadataValidationEventFailed(this, item, msg);
  }
}
