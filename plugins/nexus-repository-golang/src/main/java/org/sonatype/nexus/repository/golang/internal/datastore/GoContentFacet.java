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
package org.sonatype.nexus.repository.golang.internal.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.golang.AssetKind;
import org.sonatype.nexus.repository.golang.GolangFormat;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangAttributes;
import org.sonatype.nexus.repository.golang.internal.metadata.GolangInfo;
import org.sonatype.nexus.repository.golang.internal.util.CompressedContentExtractor;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.golang.AssetKind.MODULE;
import static org.sonatype.nexus.repository.golang.AssetKind.PACKAGE;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;
import static org.sonatype.nexus.repository.view.Payload.UNKNOWN_SIZE;

/**
 * Golang content facet.
 *
 * @since 3.next
 */
@Exposed
@Named(GolangFormat.NAME)
public class GoContentFacet
    extends ContentFacetSupport
{
  private static final String GO_MOD_FILENAME = "go.mod";

  private static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1);

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final CompressedContentExtractor compressedContentExtractor;

  @Inject
  public GoContentFacet(
      @Named(GolangFormat.NAME) final FormatStoreManager formatStoreManager,
      final CompressedContentExtractor compressedContentExtractor)
  {
    super(formatStoreManager);
    this.compressedContentExtractor = checkNotNull(compressedContentExtractor);
  }

  /**
   * Upload Go's ZIP file to the repository.
   *
   * @param path             the path of the package.
   * @param golangAttributes the Go specific attributes.
   * @param payload          the {@link Payload} of ZIP file.
   */
  public void upload(final String path, final GolangAttributes golangAttributes, final Payload payload)
  {
    checkNotNull(path);
    checkNotNull(golangAttributes);
    checkNotNull(payload);

    FluentAsset packageAsset = saveComponentAndAsset(path, payload, PACKAGE, golangAttributes);
    extractAndSaveMod(packageAsset, golangAttributes);
  }

  /**
   * Get a list of asset versions.
   *
   * @param module the module name.
   * @return {@link Content} with all available versions.
   */
  public Optional<Content> getVersions(final String module) {
    checkNotNull(module);

    Iterable<FluentComponent> components = getComponents(module);

    List<String> versions = StreamSupport.stream(components.spliterator(), false)
        .map(Component::version)
        .collect(Collectors.toList());

    if (versions.isEmpty()) {
      return Optional.empty();
    }

    String listOfVersions = String.join("\n", versions);
    Payload payload = new BytesPayload(listOfVersions.getBytes(UTF_8), TEXT_PLAIN);
    Content content = new Content(payload);
    return Optional.of(content);
  }

  /**
   * Get FluentAsset by asset path
   *
   * @param assetPath the asset path.
   * @return the {@link FluentAsset}.
   */
  public Optional<FluentAsset> getAsset(final String assetPath) {
    checkNotNull(assetPath);
    return assets().path(normalizeAssetPath(assetPath)).find();
  }

  /**
   * Get info of the package. Contains version and time of creation.
   *
   * @param path             the path of the package.
   * @param golangAttributes Go specific attributes.
   * @return the info about package.
   */
  public Optional<Content> getInfo(final String path, final GolangAttributes golangAttributes)
  {
    checkNotNull(path);
    checkNotNull(golangAttributes);
    String newPath = getZipAssetPathFromInfoPath(path);

    return assets()
        .path(normalizeAssetPath(newPath))
        .find()
        .flatMap(Asset::blob)
        .map(assetBlob -> doGetInfo(assetBlob.blobCreated(), golangAttributes))
        .map(Content::new);
  }

  private Payload doGetInfo(final OffsetDateTime blobCreated, final GolangAttributes goAttributes) {
    GolangInfo goInfo = new GolangInfo(goAttributes.getVersion(), blobCreated.toString());
    try {
      String info = MAPPER.writeValueAsString(goInfo);
      byte[] bytes = info.getBytes(UTF_8);
      return new BytesPayload(bytes, APPLICATION_JSON);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(format("Unable to convert %s to json", goInfo), e); // NOSONAR
    }
  }

  /**
   * Rather than reading the stream once to decide if the file is present and then to also pass that stream in to the
   * {@code StreamPayload} from {@code getModAsPayload}, it is read separately. Reading the stream twice prevents the
   * possibility of the stream getting closed before the {@code StreamPayload} has had chance to read the content.
   */
  private void extractAndSaveMod(final FluentAsset packageAsset, final GolangAttributes golangAttributes) {
    Payload content = packageAsset.download();
    String packageAssetPath = packageAsset.path();

    if (goModExistsInZip(content, packageAssetPath)) {
      String moduleAssetPath = packageAssetPath.replace(".zip", ".mod");
      Payload payload = getModAsPayload(content, packageAssetPath);
      saveComponentAndAsset(moduleAssetPath, payload, MODULE, golangAttributes);
    }
  }

  private boolean goModExistsInZip(final Payload content, final String path) {
    return compressedContentExtractor.fileExists(content, path, GO_MOD_FILENAME);
  }

  private Payload getModAsPayload(final Payload content, final String path) {
    return new StreamPayload(() -> doGetModAsStream(content, path), UNKNOWN_SIZE, TEXT_PLAIN);
  }

  private String getZipAssetPathFromInfoPath(final String path) {
    return path.replace(".info", ".zip");
  }

  private InputStream doGetModAsStream(final Payload content, final String path) {
    try (InputStream contentStream = content.openInputStream();
         InputStream inputStream = compressedContentExtractor.extractFile(contentStream, GO_MOD_FILENAME)) {
      checkNotNull(inputStream, format("Unable to find file %s in %s", GO_MOD_FILENAME, path));
      return inputStream;
    }
    catch (IOException e) {
      throw new RuntimeException(format("Unable to open content %s", path), e); // NOSONAR
    }
  }

  /**
   * Saves a new component and asset
   *
   * @param path             the asset path of an asset
   * @param payload          the payload
   * @param assetKind        the asset kind
   * @param golangAttributes the specific go attributes of the Go structure.
   * @return {@link FluentAsset}
   */
  public FluentAsset saveComponentAndAsset(
      final String path,
      final Payload payload,
      final AssetKind assetKind,
      final GolangAttributes golangAttributes)
  {
    FluentComponent component = findOrCreateComponent(golangAttributes.getModule(), golangAttributes.getVersion());
    try (TempBlob tempBlob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return assets()
          .path(normalizeAssetPath(path))
          .kind(assetKind.name())
          .blob(tempBlob)
          .component(component)
          .save();
    }
  }

  /**
   * Get or create {@link FluentComponent}; if it doesn't exist then it is created.
   *
   * @param name    the component name.
   * @param version the component version.
   * @return the {@link FluentComponent} object.
   */
  private FluentComponent findOrCreateComponent(final String name, final String version) {
    checkNotNull(name);
    checkNotNull(version);

    return components()
        .name(name)
        .version(version)
        .getOrCreate();
  }

  /**
   * Saves a new asset
   *
   * @param path      the asset path
   * @param payload   the payload
   * @param assetKind the asset kind
   * @return {@link FluentAsset}
   */
  public FluentAsset saveAsset(
      final String path,
      final Payload payload,
      final AssetKind assetKind)
  {
    try (TempBlob tempBlob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return assets()
          .path(normalizeAssetPath(path))
          .kind(assetKind.name())
          .blob(tempBlob)
          .save();
    }
  }

  private Iterable<FluentComponent> getComponents(final String module) {
    String filterString = "name = #{" + ComponentDAO.FILTER_PARAMS + ".componentNameParam}";
    Map<String, Object> params = Collections.singletonMap("componentNameParam", module);
    return iterableOf(
        components().byFilter(filterString, params)::browse
    );
  }

  private String normalizeAssetPath(final String path) {
    return StringUtils.prependIfMissing(path, "/");
  }
}
