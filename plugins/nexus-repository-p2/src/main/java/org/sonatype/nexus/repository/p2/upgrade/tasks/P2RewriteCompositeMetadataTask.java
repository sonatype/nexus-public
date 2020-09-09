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
package org.sonatype.nexus.repository.p2.upgrade.tasks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.mime.ContentValidator;
import org.sonatype.nexus.repository.p2.P2Facet;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.metadata.CompositeRepositoryRewriter;
import org.sonatype.nexus.repository.p2.internal.proxy.P2ProxyFacetImpl;
import org.sonatype.nexus.repository.p2.internal.proxy.StreamCopier;
import org.sonatype.nexus.repository.p2.internal.util.P2PathUtils;
import org.sonatype.nexus.repository.p2.upgrade.LegacyPathUtil;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static org.sonatype.nexus.repository.p2.internal.P2FacetImpl.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.p2.upgrade.P2Upgrade_1_2.MARKER_FILE;

/**
 * Rewrites composite metadata to reference site hashes rather than URL paths.
 *
 * @since 1.1
 */
@Named
public class P2RewriteCompositeMetadataTask
    extends TaskSupport
    implements Cancelable
{
  private final Path markerFile;

  private final RepositoryManager repositoryManager;

  private final BucketStore bucketStore;

  private final ContentValidator contentValidator;

  private final TaskScheduler taskScheduler;

  @Inject
  public P2RewriteCompositeMetadataTask(
      final ApplicationDirectories directories,
      final BucketStore bucketStore,
      final RepositoryManager repositoryManager,
      final TaskScheduler taskScheduler,
      final ContentValidator contentValidator)
  {
    markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE).toPath();
    this.bucketStore = checkNotNull(bucketStore);
    this.contentValidator = checkNotNull(contentValidator);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @Override
  protected Object execute() throws Exception {
    List<Repository> repositories = stream(repositoryManager.browse())
        .peek(r -> log.debug("Looking at repository: {}", r))
        .filter(r -> r.getFormat() instanceof P2Format)
        .peek(r -> log.debug("Looking at p2 repository: {}", r))
        .filter(r -> r.getType() instanceof ProxyType)
        .peek(r -> log.debug("Found p2 proxy repository: {}", r))
        .collect(Collectors.toList());

    if (!repositories.isEmpty()) {
      Iterable<Asset> compositeAssets = rewriteLegacyAssets(repositories);
      CancelableHelper.checkCancellation();

      compositeAssets.forEach(asset -> {
        Optional<Repository> optRepository = getRepositoryForBucketId(asset.bucketId());
        if (!optRepository.isPresent()) {
          log.warn("Unable to locate repository for asset {}", EntityHelper.id(asset));
          return;
        }
        Repository repository = optRepository.get();

        UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
        try {
          rewriteAsset(repository, asset);
        }
        finally {
          UnitOfWork.end();
        }
      });
      rebuildBrowse(repositories);
    }

    if (Files.exists(markerFile)) {
      Files.delete(markerFile);
    }
    return null;
  }

  private Iterable<Asset> rewriteLegacyAssets(final List<Repository> repositories) {
    log.info("Rewriting legacy assets in p2 proxy repository: {}", repositories);

    return Transactional.operation.withDb(repositories.get(0).facet(StorageFacet.class).txSupplier()).call(() -> {
      final StorageTx tx = UnitOfWork.currentTx();
      return tx.findAssets("attributes.p2.asset_kind IN :kinds",
          Collections.singletonMap("kinds", Arrays.asList(AssetKind.COMPOSITE_ARTIFACTS, AssetKind.COMPOSITE_CONTENT)),
          repositories, null);
    });
  }

  @TransactionalStoreBlob
  protected void rewriteAsset(final Repository repository, final Asset site) {
    log.debug("Rewriting {} in {}", site.name(), repository.getName());

    StorageFacet storageFacet = repository.facet(StorageFacet.class);
    boolean isRoot = !site.name().contains("/");
    URI baseUri = isRoot ? getRepositoryUri(repository) : URI.create(site.formatAttributes().get(P2ProxyFacetImpl.REMOTE_URL, String.class));

    try {
      Content content = getContent(repository, site);

      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      try (InputStream in = content.openInputStream()) {
        IOUtils.copy(in, buffer);
      }

      ByteArrayInputStream in = new ByteArrayInputStream(buffer.toByteArray());
      buffer.reset();

      String mimeType = contentValidator.determineContentType(false, () -> in, null, site.name(), content.getContentType());
      in.reset();
      CompositeRepositoryRewriter rewriter = new CompositeRepositoryRewriter(baseUri, isRoot, LegacyPathUtil::unescapePathToUri);
      String internalFilename = P2PathUtils.getAssetKind(site.name()) == AssetKind.COMPOSITE_ARTIFACTS ? "compositeArtifacts.xml" : "compositeContent.xml";
      StreamCopier.copierFor(mimeType, internalFilename, in, buffer).process(rewriter);

      try (ByteArrayInputStream rewritten = new ByteArrayInputStream(buffer.toByteArray());
          TempBlob metadataContent = storageFacet.createTempBlob(rewritten, HASH_ALGORITHMS)) {
        site.formatAttributes().set(P2ProxyFacetImpl.CHILD_URLS, rewriter.getUrls());

        repository.facet(P2Facet.class).saveAsset(UnitOfWork.currentTx(), site, metadataContent, content);
      }
    }
    catch (IOException e) {
      log.error("Failed to migrate {} in {}", site.name(), repository.getName(), e);
    }
  }

  @TransactionalTouchBlob
  protected Content getContent(final Repository repository, final Asset asset) {
    StorageTx tx = UnitOfWork.currentTx();

    return repository.facet(P2Facet.class).toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private URI getRepositoryUri(final Repository repository) {
    URI uri = repository.facet(ProxyFacet.class).getRemoteUrl();
    String uriString = uri.toString();
    if (uri.toString().endsWith("/")) {
      return uri;
    }
    return URI.create(uriString + '/');
  }

  private Optional<Repository> getRepositoryForBucketId(final EntityId bucketId) {
    return Optional.ofNullable(bucketStore.getById(bucketId)).map(Bucket::getRepositoryName).map(repositoryManager::get);
  }

  @Override
  public String getMessage() {
    return "Rewrite p2 composite site metadata";
  }

  private void rebuildBrowse(final List<Repository> repositories) {
    String repositoryNames = repositories.stream().map(Repository::getName).collect(Collectors.joining(","));

    boolean existingTask = taskScheduler.findAndSubmit(RebuildBrowseNodesTaskDescriptor.TYPE_ID,
        ImmutableMap.of(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryNames));
    if (!existingTask) {
      TaskConfiguration configuration = taskScheduler
          .createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
      configuration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, repositoryNames);
      configuration.setName("Rebuild repository browse tree - (" + repositoryNames + ")");
      taskScheduler.submit(configuration);
    }
  }
}
