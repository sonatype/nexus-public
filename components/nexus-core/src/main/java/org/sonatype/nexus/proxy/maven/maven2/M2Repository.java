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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.AbstractMavenRepository;
import org.sonatype.nexus.proxy.maven.MavenRepositoryMetadataManager;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.GavCalculator;
import org.sonatype.nexus.proxy.maven.gav.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryMetadataManager;
import org.sonatype.nexus.util.AlphanumComparator;
import org.sonatype.nexus.util.DigesterUtils;
import org.sonatype.nexus.util.io.StreamSupport;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.sisu.Description;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default M2Repository.
 *
 * @author cstamas
 */
@Named(M2Repository.ID)
@Typed(Repository.class)
@Description("Maven2 Repository")
public class M2Repository
    extends AbstractMavenRepository
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

  private final M2RepositoryConfigurator m2RepositoryConfigurator;

  private final MavenRepositoryMetadataManager mavenRepositoryMetadataManager;

  @Inject
  public M2Repository(final @Named(Maven2ContentClass.ID) ContentClass contentClass,
                      final @Named("maven2") GavCalculator gavCalculator,
                      final M2RepositoryConfigurator m2RepositoryConfigurator)
  {
    this.contentClass = checkNotNull(contentClass);
    this.gavCalculator = checkNotNull(gavCalculator);
    this.m2RepositoryConfigurator = checkNotNull(m2RepositoryConfigurator);
    this.mavenRepositoryMetadataManager = new MavenRepositoryMetadataManager(this);
  }

  @Override
  public RepositoryMetadataManager getRepositoryMetadataManager() {
    return mavenRepositoryMetadataManager;
  }

  @Override
  protected M2RepositoryConfiguration getExternalConfiguration(boolean forWrite) {
    return (M2RepositoryConfiguration) super.getExternalConfiguration(forWrite);
  }

  @Override
  protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
    return new CRepositoryExternalConfigurationHolderFactory<M2RepositoryConfiguration>()
    {
      public M2RepositoryConfiguration createExternalConfigurationHolder(CRepository config) {
        return new M2RepositoryConfiguration((Xpp3Dom) config.getExternalConfiguration());
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
    return m2RepositoryConfigurator;
  }

  @Override
  public boolean isMavenMetadataPath(String path) {
    return M2ArtifactRecognizer.isMetadata(path);
  }

  @Override
  public boolean isMavenArtifactChecksumPath(String path) {
    return M2ArtifactRecognizer.isChecksum(path);
  }

  /**
   * Should serve by policies.
   *
   * @param request the request
   * @return true, if successful
   */
  @Override
  public boolean shouldServeByPolicies(ResourceStoreRequest request) {
    if (M2ArtifactRecognizer.isMetadata(request.getRequestPath())) {
      if (M2ArtifactRecognizer.isSnapshot(request.getRequestPath())) {
        return RepositoryPolicy.SNAPSHOT.equals(getRepositoryPolicy());
      }
      else {
        // metadatas goes always
        return true;
      }
    }

    // we are using Gav to test the path
    final Gav gav = getGavCalculator().pathToGav(request.getRequestPath());

    if (gav == null) {
      return true;
    }
    else {
      if (gav.isSnapshot()) {
        // snapshots goes if enabled
        return RepositoryPolicy.SNAPSHOT.equals(getRepositoryPolicy());
      }
      else {
        return RepositoryPolicy.RELEASE.equals(getRepositoryPolicy());
      }
    }
  }

  @Override
  public AbstractStorageItem doCacheItem(AbstractStorageItem item)
      throws LocalStorageException
  {
    // if the item is file, is M2 repository metadata and this repo is release-only or snapshot-only
    if (isCleanseRepositoryMetadata() && item instanceof StorageFileItem
        && M2ArtifactRecognizer.isMetadata(item.getPath())) {
      StorageFileItem mdFile = (StorageFileItem) item;
      ByteArrayInputStream backup = null;
      ByteArrayOutputStream backup1 = new ByteArrayOutputStream();
      try {
        // remote item is not reusable, and we usually cache remote stuff locally
        try (final InputStream orig = mdFile.getInputStream()) {
          StreamSupport.copy(orig, backup1, StreamSupport.BUFFER_SIZE);
        }
        backup = new ByteArrayInputStream(backup1.toByteArray());

        // Metadata is small, let's do it in memory
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();
        InputStreamReader isr = new InputStreamReader(backup);
        Metadata imd = metadataReader.read(isr);

        // and fix it
        imd = cleanseMetadataForRepository(RepositoryPolicy.SNAPSHOT.equals(getRepositoryPolicy()), imd);

        // serialize and swap the new metadata
        MetadataXpp3Writer metadataWriter = new MetadataXpp3Writer();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bos);
        metadataWriter.write(osw, imd);
        mdFile.setContentLocator(new ByteArrayContentLocator(bos.toByteArray(), mdFile.getMimeType()));
      }
      catch (Exception e) {
        log.error("Exception during repository metadata cleansing.", e);

        if (backup != null) {
          // get backup and continue operation
          backup.reset();
          mdFile.setContentLocator(new ByteArrayContentLocator(backup1.toByteArray(), mdFile.getMimeType()));
        }
      }
    }

    return super.doCacheItem(item);
  }

  @Override
  protected boolean isOld(StorageItem item) {
    if (M2ArtifactRecognizer.isMetadata(item.getPath())) {
      return isOld(getMetadataMaxAge(), item);
    }

    // we are using Gav to test the path
    final Gav gav = getGavCalculator().pathToGav(item.getPath());

    if (gav != null && gav.isSnapshot()) {
      return isOld(getArtifactMaxAge(), item);
    }

    if (gav == null) {
      // this is not an artifact, it is just any "file"
      return super.isOld(item);
    }
    // it is a release
    return isOld(getArtifactMaxAge(), item);
  }

  protected Metadata cleanseMetadataForRepository(boolean snapshot, Metadata metadata) {
    // remove base versions not belonging here
    List<String> versions = metadata.getVersioning().getVersions();
    for (Iterator<String> iversion = versions.iterator(); iversion.hasNext(); ) {
      // if we need snapshots and the version is not snapshot, or
      // if we need releases and the version is snapshot
      if ((snapshot && !Gav.isSnapshot(iversion.next()))
          || (!snapshot && Gav.isSnapshot(iversion.next()))) {
        iversion.remove();
      }
    }

    metadata.getVersioning().setLatest(getLatestVersion(metadata.getVersioning().getVersions()));
    if (snapshot) {
      metadata.getVersioning().setRelease(null);
    }
    else {
      metadata.getVersioning().setRelease(metadata.getVersioning().getLatest());
    }
    return metadata;
  }

  public String getLatestVersion(List<String> versions) {
    Collections.sort(versions, new AlphanumComparator());

    return versions.get(versions.size() - 1);
  }

  @Override
  protected void enforceWritePolicy(ResourceStoreRequest request, Action action)
      throws IllegalRequestException
  {
    // allow updating of metadata
    // we also need to allow updating snapshots
    if (!M2ArtifactRecognizer.isMetadata(request.getRequestPath())) {
      Gav gav = getGavCalculator().pathToGav(request.getRequestPath());
      if (gav == null || !gav.isSnapshot()) {
        super.enforceWritePolicy(request, action);
      }
    }
  }

  @Override
  protected StorageItem doRetrieveItem(ResourceStoreRequest request)
      throws IllegalOperationException, ItemNotFoundException, StorageException
  {
    // we do all this mockery (NEXUS-4218 and NEXUS-4243) ONLY when we are sure
    // that we deal with REMOTE REQUEST INITIATED BY OLD 2.x MAVEN and nothing else.
    // Before this fix, the code was executed whenever
    // "!ModelVersionUtility.LATEST_MODEL_VERSION.equals( userSupportedVersion ) )" was true
    // and it was true even for userSupportedVersion being null (ie. not user agent string supplied!)!!!
    final boolean remoteCall = request.getRequestContext().containsKey(AccessManager.REQUEST_REMOTE_ADDRESS);
    final String userAgent = (String) request.getRequestContext().get(AccessManager.REQUEST_AGENT);

    if (remoteCall && null != userAgent) {
      final Version userSupportedVersion = getClientSupportedVersion(userAgent);

      // we still can make up our mind here, we do this only if we know: this request is about metadata,
      // the client's metadata version is known and it is not the latest one
      if (M2ArtifactRecognizer.isMetadata(request.getRequestPath()) && userSupportedVersion != null
          && !ModelVersionUtility.LATEST_MODEL_VERSION.equals(userSupportedVersion)) {
        // metadata checksum files are calculated and cached as side-effect
        // of doRetrieveMetadata.
        final StorageFileItem mdItem;
        if (M2ArtifactRecognizer.isChecksum(request.getRequestPath())) {
          String path = request.getRequestPath();
          if (request.getRequestPath().endsWith(".md5")) {
            path = path.substring(0, path.length() - 4);
          }
          else if (request.getRequestPath().endsWith(".sha1")) {
            path = path.substring(0, path.length() - 5);
          }
          // we have to keep original reqest's flags: localOnly and remoteOnly are strange ones, so
          // we do a hack here
          // second, since we initiate a request for different path within a context of this request,
          // we need to be careful about it
          ResourceStoreRequest mdRequest =
              new ResourceStoreRequest(path, request.isRequestLocalOnly(), request.isRequestRemoteOnly());
          mdRequest.getRequestContext().setParentContext(request.getRequestContext());

          mdItem = (StorageFileItem) super.retrieveItem(false, mdRequest);
        }
        else {
          mdItem = (StorageFileItem) super.doRetrieveItem(request);
        }

        try {
          Metadata metadata;
          try (final InputStream inputStream = mdItem.getInputStream()) {
            metadata = MetadataBuilder.read(inputStream);
          }

          Version requiredVersion = getClientSupportedVersion(userAgent);
          Version metadataVersion = ModelVersionUtility.getModelVersion(metadata);

          if (requiredVersion == null || requiredVersion.equals(metadataVersion)) {
            return super.doRetrieveItem(request);
          }

          ModelVersionUtility.setModelVersion(metadata, requiredVersion);

          ByteArrayOutputStream mdOutput = new ByteArrayOutputStream();

          MetadataBuilder.write(metadata, mdOutput);

          final byte[] content;
          if (M2ArtifactRecognizer.isChecksum(request.getRequestPath())) {
            String digest;
            if (request.getRequestPath().endsWith(".md5")) {
              digest = DigesterUtils.getMd5Digest(mdOutput.toByteArray());
            }
            else {
              digest = DigesterUtils.getSha1Digest(mdOutput.toByteArray());
            }
            content = (digest + '\n').getBytes("UTF-8");
          }
          else {
            content = mdOutput.toByteArray();
          }

          String mimeType =
              getMimeSupport().guessMimeTypeFromPath(getMimeRulesSource(), request.getRequestPath());
          ContentLocator contentLocator = new ByteArrayContentLocator(content, mimeType);

          DefaultStorageFileItem result =
              new DefaultStorageFileItem(this, request, true, false, contentLocator);
          result.setCreated(mdItem.getCreated());
          result.setModified(System.currentTimeMillis());
          return result;
        }
        catch (IOException e) {
          if (log.isDebugEnabled()) {
            log.error("Error parsing metadata, serving as retrieved", e);
          }
          else {
            log.error("Error parsing metadata, serving as retrieved: " + e.getMessage());
          }

          return super.doRetrieveItem(request);
        }
      }
    }

    return super.doRetrieveItem(request);
  }

  protected Version getClientSupportedVersion(String userAgent) {
    if (userAgent == null) {
      return null;
    }

    if (userAgent.startsWith("Apache Ivy")) {
      return Version.V100;
    }

    if (userAgent.startsWith("Java")) {
      return Version.V100;
    }

    if (userAgent.startsWith("Apache-Maven/2")) {
      return Version.V100;
    }

    return Version.V110;
  }

}
