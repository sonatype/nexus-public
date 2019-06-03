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
package org.sonatype.nexus.repository.golang.internal.hosted;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangInfo;
import org.sonatype.nexus.repository.golang.internal.util.CompressedContentExtractor;
import org.sonatype.nexus.repository.golang.internal.util.GolangDataAccess;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.repository.golang.AssetKind.MODULE;
import static org.sonatype.nexus.repository.golang.AssetKind.PACKAGE;
import static org.sonatype.nexus.repository.golang.internal.util.GolangDataAccess.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;
import static org.sonatype.nexus.repository.view.Payload.UNKNOWN_SIZE;

/**
 * Go hosted implementation
 *
 * @since 3.next
 */
@Named
public class GolangHostedFacetImpl
    extends FacetSupport
    implements GolangHostedFacet
{
  private static final String GO_MOD_FILENAME = "go.mod";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final GolangDataAccess golangDataAccess;

  private final CompressedContentExtractor compressedContentExtractor;

  @Inject
  public GolangHostedFacetImpl(final GolangDataAccess golangDataAccess,
                               final CompressedContentExtractor compressedContentExtractor)
  {
    this.golangDataAccess = checkNotNull(golangDataAccess);
    this.compressedContentExtractor = checkNotNull(compressedContentExtractor);
  }

  @Override
  @Nullable
  @Transactional
  public Content getInfo(final String path,
                         final GolangAttributes golangAttributes)
  {
    checkNotNull(path);
    checkNotNull(golangAttributes);
    String newPath = getZipAssetPathFromInfoPath(path);

    StreamPayload streamPayload = extractInfoFromZip(golangAttributes, newPath);
    if (streamPayload == null) {
      return null;
    }
    return new Content(streamPayload);
  }

  @Transactional
  @Override
  public Content getList(final String module) {
    checkNotNull(module);

    StorageTx tx = UnitOfWork.currentTx();

    Iterable<Asset> assetsForModule = golangDataAccess.findAssetsForModule(tx, getRepository(), module);

    List<String> collection = StreamSupport.stream(assetsForModule.spliterator(), false)
        .filter(asset -> PACKAGE.name().equals(asset.formatAttributes().get(P_ASSET_KIND)))
        .map(Asset::name)
        .map(name -> name.split("/@v/")[1])
        .map(name -> name.replaceAll(".zip", ""))
        .collect(Collectors.toList());

    if (collection.isEmpty()) {
      return null;
    }

    String listOfVersions = String.join("\n", collection);

    return new Content(new BytesPayload(listOfVersions.getBytes(UTF_8), TEXT_PLAIN));
  }

  @Nullable
  @TransactionalTouchBlob
  @Override
  public Content getPackage(final String path) {
    checkNotNull(path);
    return doGet(path);
  }

  @Nullable
  @TransactionalTouchBlob
  @Override
  public Content getMod(final String path) {
    checkNotNull(path);
    return doGet(path);
  }

  @Override
  public void upload(final String path,
                     final GolangAttributes golangAttributes,
                     final Payload payload,
                     final AssetKind assetKind) throws IOException
  {
    checkNotNull(path);
    checkNotNull(golangAttributes);
    checkNotNull(payload);
    checkNotNull(assetKind);

    if (assetKind != PACKAGE) {
      throw new IllegalArgumentException("Unsupported AssetKind");
    }

    storeContent(path, golangAttributes, payload, assetKind);
    extractAndSaveMod(path, golangAttributes);
  }

  private String getZipAssetPathFromInfoPath(final String path) {
    return path.replaceAll("\\.info", "\\.zip");
  }

  private StreamPayload extractInfoFromZip(final GolangAttributes goAttributes, final String newPath) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = golangDataAccess.findAsset(tx, tx.findBucket(getRepository()), newPath);
    if (asset == null) {
      return null;
    }

    return new StreamPayload(
        () -> doGetInfo(asset, goAttributes),
        UNKNOWN_SIZE,
        APPLICATION_JSON);
  }

  private InputStream doGetInfo(final Asset asset, final GolangAttributes goAttributes) {
    GolangInfo goInfo = new GolangInfo(goAttributes.getVersion(), asset.blobCreated().toString());
    try {
      String info = MAPPER.writeValueAsString(goInfo);
      return new ByteArrayInputStream(info.getBytes(UTF_8));
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(format("Unable to convert %s to json", goInfo), e);
    }
  }

  private Content doGet(final String path) {
    checkNotNull(path);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = golangDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }

    return golangDataAccess.toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private void storeContent(final String path,
                            final GolangAttributes golangAttributes,
                            final Payload payload,
                            final AssetKind assetKind) throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload.openInputStream(), HASH_ALGORITHMS)) {
      golangDataAccess
          .maybeCreateAndSaveComponent(getRepository(), golangAttributes, path, tempBlob, payload, assetKind);
    }
  }

  /**
   * Rather than reading the stream once to decide if the file is present and then to also
   * pass that stream in to the {@code StreamPayload} from {@code getModAsPayload}, it is read
   * separately. Reading the stream twice prevents the possibility of the stream getting closed
   * before the {@code StreamPayload} has had chance to read the content.
   */
  private void extractAndSaveMod(final String path, final GolangAttributes golangAttributes) {
    Payload content = getZip(path);

    if (content != null && goModExistsInZip(content, path)) {
      try {
        storeContent(path.replaceAll("\\.zip", "\\.mod"),
            golangAttributes,
            getModAsPayload(content, path),
            MODULE);
      }
      catch (IOException e) {
        log.warn("Unable to open content {}", path, e);
      }
    }
  }

  private Payload getModAsPayload(final Payload content, final String path) {
    return new StreamPayload(() -> doGetModAsStream(content, path),
        UNKNOWN_SIZE, TEXT_PLAIN);
  }

  private InputStream doGetModAsStream(final Payload content, final String path) {
    try (InputStream contentStream = content.openInputStream()) {
      InputStream inputStream = compressedContentExtractor.extractFile(contentStream, GO_MOD_FILENAME);
      checkNotNull(inputStream, format("Unable to find file %s in %s", GO_MOD_FILENAME, path));
      return inputStream;
    }
    catch (IOException e) {
      throw new RuntimeException(format("Unable to open content %s", path), e);
    }
  }

  private boolean goModExistsInZip(final Payload content, final String path) {
    return compressedContentExtractor.fileExists(content, path, GO_MOD_FILENAME);
  }

  @TransactionalTouchBlob
  protected Payload getZip(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = golangDataAccess.findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      log.warn("Unable to find {} for extraction", path);
      return null;
    }
    return golangDataAccess.getBlobAsPayload(tx, asset);
  }
}
