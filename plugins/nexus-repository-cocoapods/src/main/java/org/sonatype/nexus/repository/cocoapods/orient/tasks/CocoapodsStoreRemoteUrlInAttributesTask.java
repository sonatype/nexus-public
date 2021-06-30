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
package org.sonatype.nexus.repository.cocoapods.orient.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat;
import org.sonatype.nexus.repository.cocoapods.internal.PathUtils;
import org.sonatype.nexus.repository.cocoapods.internal.proxy.InvalidSpecFileException;
import org.sonatype.nexus.repository.cocoapods.internal.proxy.SpecFileProcessor;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.Query.Builder;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat.POD_REMOTE_ATTRIBUTE_NAME;
import static org.sonatype.nexus.repository.cocoapods.orient.upgrade.CocoapodsUpgrade_1_1.MARKER_FILE;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;

/**
 * NEXUS-24835: Move download URL from pod asset name to the format attribute of the spec file".
 *
 * @since 3.27
 */
@Named
public class CocoapodsStoreRemoteUrlInAttributesTask
    extends TaskSupport
    implements Cancelable
{
  private static final int SPEC_ASSET_PATH_NAME_INDEX = 4;

  private static final int SPEC_ASSET_PATH_VERSION_INDEX = 5;

  private final Path markerFile;

  private final RepositoryManager repositoryManager;

  private final SpecFileProcessor specFileProcessor;

  @Inject
  public CocoapodsStoreRemoteUrlInAttributesTask(
      final ApplicationDirectories directories,
      final RepositoryManager repositoryManager,
      final SpecFileProcessor specFileProcessor)
  {
    this.markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE).toPath();
    this.repositoryManager = checkNotNull(repositoryManager);
    this.specFileProcessor = specFileProcessor;
  }

  @Override
  protected Object execute() throws Exception {
    List<Repository> repositories = getCocoapodsRepositories();
    repositories.forEach((repository -> {
      StorageFacet storageFacet = repository.facet(StorageFacet.class);

      try (StorageTx tx = storageFacet.txSupplier().get()) {
        tx.begin();
        updateRepositoryData(tx, repository);
        tx.commit();
      }
    }));
    if (Files.exists(markerFile)) {
      Files.delete(markerFile);
    }
    return null;
  }

  @Override
  public String getMessage() {
    return "Cocoapod. Store remoteDownloadURL in SPEC attributes, update pod assets name.";
  }

  private List<Repository> getCocoapodsRepositories() {
    return stream(repositoryManager.browse())
        .filter(r -> {
          if (r.getFormat() instanceof CocoapodsFormat && r.getType().getValue().equals(ProxyType.NAME)) {
            log.debug("Looking at Cocoapods repository: {}", r);
            return true;
          }
          return false;
        })
        .collect(Collectors.toList());
  }

  private void updateRepositoryData(final StorageTx tx, final Repository repository) {
    log.debug("Update nested assets in {}", repository.getName());

    getSpecFiles(tx, repository).forEach(asset -> {
      try {
        log.debug("Update package {}", asset.name());
        updateCocoapodsPackage(tx, asset, repository);
      }
      catch (InvalidSpecFileException ise) {
        log.warn("Invalid spec file: {}; {}", asset.name(), ise.getMessage());
        tx.deleteAsset(asset);
      }
      catch (Exception e) {
        log.error("error updating package: " + asset.name(), e);
      }
    });
  }

  private void updateCocoapodsPackage(final StorageTx tx, final Asset specAsset, final Repository repository)
      throws IOException, InvalidSpecFileException
  {
    String remoteDownloadURL = extractRemoteDownloadURL(tx, specAsset);

    specAsset.formatAttributes().set(POD_REMOTE_ATTRIBUTE_NAME, remoteDownloadURL);
    tx.saveAsset(specAsset);

    CocoapodsCoords coords = parseSpecPath(specAsset.name());

    updatePodFile(tx, repository, coords, remoteDownloadURL);
  }

  private void updatePodFile(final StorageTx tx,
                             final Repository repository,
                             final CocoapodsCoords coords,
                             final String remoteDownloadURL)
  {
    Asset podAsset = findPodFile(tx, repository, coords).orElse(null);
    if (podAsset != null) {
      String podPath = PathUtils.buildNxrmPodPath(coords.name, coords.version, URI.create(remoteDownloadURL));
      log.debug("Update pod: {}", podPath);
      podAsset.name(podPath);
      tx.saveAsset(podAsset);
    }
  }

  private String extractRemoteDownloadURL(final StorageTx tx, final Asset specAsset)
      throws InvalidSpecFileException, IOException
  {
    Blob spec = tx.requireBlob(specAsset.requireBlobRef());

    String specContent;
    try (InputStream is = spec.getInputStream()) {
      specContent = IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    return specFileProcessor.extractExternalUri(specContent).toString();
  }

  private Iterable<Asset> getSpecFiles(final StorageTx tx, final Repository repository) {
    Builder builder = builder()
        .where(P_NAME).like("Specs/%");
    Query query = builder.build();

    return tx.browseAssets(query, tx.findBucket(repository));
  }

  private Optional<Asset> findPodFile(final StorageTx tx, final Repository repository, final CocoapodsCoords coords) {
    String nameTerm = String.format("pods/%s/%s/", coords.name, coords.version) + "%";

    Builder builder = builder()
        .where(P_NAME).like(nameTerm);
    Query query = builder.build();

    return Streams.stream(tx.browseAssets(query, tx.findBucket(repository))).findAny();
  }

  private CocoapodsCoords parseSpecPath(final String specPath) throws InvalidSpecFileException {
    String[] segments = specPath.split("/");
    if (segments.length != 7) {
      throw new InvalidSpecFileException("Invalid Spec file name: " + specPath);
    }
    return new CocoapodsCoords(segments[SPEC_ASSET_PATH_NAME_INDEX], segments[SPEC_ASSET_PATH_VERSION_INDEX]);
  }

  private static class CocoapodsCoords
  {
    private final String name;

    private final String version;

    CocoapodsCoords(String name, final String version) {
      this.name = name;
      this.version = version;
    }
  }
}
