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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.validation.MavenMetadataContentValidator;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableMap;
import org.apache.maven.model.Model;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.content.maven.internal.recipe.AttributesHelper.assetKind;
import static org.sonatype.nexus.content.maven.internal.recipe.AttributesHelper.getPackaging;
import static org.sonatype.nexus.content.maven.internal.recipe.AttributesHelper.setAssetAttributes;
import static org.sonatype.nexus.content.maven.internal.recipe.AttributesHelper.setPomAttributes;
import static org.sonatype.nexus.repository.maven.internal.Constants.METADATA_FILENAME;
import static org.sonatype.nexus.repository.maven.internal.MavenModels.readModel;

/**
 * A {@link MavenContentFacet} that persists to a {@link ContentFacet}.
 *
 * @since 3.25.0
 */
@Named(Maven2Format.NAME)
public class MavenContentFacetImpl
    extends ContentFacetSupport
    implements MavenContentFacet
{
  private static final String CONFIG_KEY = "maven";

  protected static final Map<HashAlgorithm, HashAlgorithm> HASHING = ImmutableMap.of(SHA1, SHA1, MD5, MD5);

  private final Map<String, MavenPathParser> mavenPathParsers;

  private final MavenMetadataContentValidator metadataValidator;

  private final boolean metadataValidationEnabled;

  private Config config;

  private MavenPathParser mavenPathParser;

  static class Config
  {
    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public VersionPolicy versionPolicy;

    @NotNull(groups = {HostedType.ValidationGroup.class, ProxyType.ValidationGroup.class})
    public LayoutPolicy layoutPolicy;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "versionPolicy=" + versionPolicy +
          ", layoutPolicy=" + layoutPolicy +
          '}';
    }
  }

  @Inject
  public MavenContentFacetImpl(
      @Named(Maven2Format.NAME) final FormatStoreManager formatStoreManager,
      final Map<String, MavenPathParser> mavenPathParsers,
      final MavenMetadataContentValidator metadataValidator,
      @Named("${nexus.maven.metadata.validation.enabled:-true}") final boolean metadataValidationEnabled)
  {
    super(formatStoreManager);
    this.mavenPathParsers = checkNotNull(mavenPathParsers);
    this.metadataValidator = metadataValidator;
    this.metadataValidationEnabled = metadataValidationEnabled;
  }

  @Override
  public Optional<Payload> get(final String path) {
    log.debug("GET {} : {}", getRepository().getName(), path);

    return assets()
        .path(path)
        .find()
        .map(FluentAsset::download);
  }

  @Override
  public Payload put(final MavenPath path, final Payload content) throws IOException {
    log.debug("PUT {} : {}", getRepository().getName(), path);

    try (TempBlob blob = blobs().ingest(content, HASHING.values())) {
      if (isMetadataAndValidationEnabled(path)) {
        validate(path, blob);
      }
      return save(path, blob);
    }
  }

  private boolean isMetadataAndValidationEnabled(final MavenPath path) {
    return path.getFileName().equals(METADATA_FILENAME) && metadataValidationEnabled;
  }

  private void validate(MavenPath mavenPath, TempBlob blob) {
    log.debug("Validating maven-metadata.xml before storing");
    metadataValidator.validate(mavenPath.getPath(), blob.get());
  }

  private Payload save(final MavenPath mavenPath, final TempBlob blob) throws IOException {
    FluentComponent component = null;
    Coordinates coordinates = mavenPath.getCoordinates();
    if (coordinates != null) {
      component = createOrGetComponent(mavenPath);
      maybeUpdateComponentAttributesFromModel(component, mavenPath, blob);
    }
    return createOrUpdateAsset(mavenPath, component, blob);
  }

  private FluentComponent createOrGetComponent(final MavenPath mavenPath)
  {
    Coordinates coordinates = mavenPath.getCoordinates();
    FluentComponent component = components()
        .name(coordinates.getArtifactId())
        .namespace(coordinates.getGroupId())
        .version(coordinates.getVersion())
        .getOrCreate();
    if (isNewRepositoryContent(component)) {
      AttributesHelper.setComponentAttributes(component, coordinates);
    }
    return component;
  }

  private void maybeUpdateComponentAttributesFromModel(
      final FluentComponent component, final MavenPath mavenPath,
      final TempBlob blob) throws IOException
  {
    Model model = maybeReadMavenModel(mavenPath, blob);
    if (model != null) {
      component.kind(getPackaging(model));
      setPomAttributes(component, model);
    }
  }

  private Model maybeReadMavenModel(final MavenPath mavenPath, final TempBlob blob) throws IOException
  {
    Model model = null;
    if (mavenPath.isPom()) {
      model = readModel(blob.getBlob().getInputStream());
      if (model == null) {
        log.warn("Could not parse POM: {} @ {}", getRepository().getName(), mavenPath.getPath());
      }
    }
    return model;
  }

  private Payload createOrUpdateAsset(
      final MavenPath path,
      final Component component,
      final TempBlob blob)
  {
    FluentAssetBuilder assetBuilder = assets().path(path.getPath()).kind(assetKind(path));
    if (component != null) {
      assetBuilder = assetBuilder.component(component);
    }
    FluentAsset asset = assetBuilder.getOrCreate();
    if (isNewRepositoryContent(asset)) {
      setAssetAttributes(asset, path);
    }
    return asset.attach(blob).download();
  }

  private boolean isNewRepositoryContent(RepositoryContent repositoryContent) {
    return repositoryContent.attributes().isEmpty();
  }

  @Override
  public boolean delete(final MavenPath mavenPath) {
    log.trace("DELETE {} : {}", getRepository().getName(), mavenPath);
    boolean assetIsDeleted = deleteAsset(mavenPath);
    if (assetIsDeleted && mavenPath.getCoordinates() != null) {
      maybeDeleteComponent(mavenPath.getCoordinates());
    }
    return assetIsDeleted;
  }

  private Boolean deleteAsset(final MavenPath mavenPath) {
    String path = mavenPath.getPath();
    return assets()
        .path(path)
        .find()
        .map(FluentAsset::delete)
        .orElse(false);
  }

  private void maybeDeleteComponent(final Coordinates coordinates) {
    components()
        .name(coordinates.getArtifactId())
        .namespace(coordinates.getGroupId())
        .version(coordinates.getVersion())
        .find()
        .ifPresent(this::deleteIfNoAssetsLeft);
  }

  private void deleteIfNoAssetsLeft(FluentComponent component) {
    if (component.assets().isEmpty()) {
      component.delete();
    }
  }

  @Override
  public MavenPathParser getMavenPathParser() {
    return mavenPathParser;
  }

  @Override
  public LayoutPolicy layoutPolicy() {
    return config.layoutPolicy;
  }

  @Override
  public VersionPolicy getVersionPolicy() {
    return config.versionPolicy;
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    mavenPathParser = checkNotNull(mavenPathParsers.get(getRepository().getFormat().getValue()));
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    super.doConfigure(configuration);
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, MavenContentFacetImpl.Config.class);
    log.debug("Config: {}", config);
  }
}
