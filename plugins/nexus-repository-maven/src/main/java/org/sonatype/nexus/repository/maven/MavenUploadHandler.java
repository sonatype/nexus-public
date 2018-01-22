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
package org.sonatype.nexus.repository.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.maven.internal.MavenPomGenerator;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.model.Model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenUploadHandler
    extends ComponentSupport
    implements UploadHandler
{
  private static final String GENERATE_POM_DISPLAY = "Generate a POM file with these coordinates";

  private static final String GENERATE_POM = "generate-pom";

  private static final String EXTENSION = "extension";

  private static final String CLASSIFIER = "classifier";

  private static final String VERSION = "version";

  private static final String ARTIFACT_ID = "artifactId";

  private static final String GROUP_ID = "groupId";

  private static final String PACKAGING = "packaging";

  private static final String ARTIFACT_ID_DISPLAY = "Artifact ID";

  private static final String GROUP_ID_DISPLAY = "Group ID";

  private static final String MAVEN_POM_PROPERTY_PREFIX = "${";

  private final ContentPermissionChecker contentPermissionChecker;

  private final Maven2MavenPathParser parser;

  private final MavenPomGenerator mavenPomGenerator;

  private UploadDefinition definition;

  private final VariableResolverAdapter variableResolverAdapter;

  private final VersionPolicyValidator versionPolicyValidator;

  @Inject
  public MavenUploadHandler(final Maven2MavenPathParser parser,
                            @Named(Maven2Format.NAME) final VariableResolverAdapter variableResolverAdapter,
                            final ContentPermissionChecker contentPermissionChecker,
                            final VersionPolicyValidator versionPolicyValidator,
                            final MavenPomGenerator mavenPomGenerator)
  {
    this.parser = parser;
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
    this.versionPolicyValidator = versionPolicyValidator;
    this.mavenPomGenerator = mavenPomGenerator;
  }

  @Override
  public Collection<String> handle(final Repository repository, final ComponentUpload upload) throws IOException {
    checkNotNull(repository);
    checkNotNull(upload);

    return doUpload(repository, upload);
  }

  private List<String> doUpload(final Repository repository, final ComponentUpload componentUpload) throws IOException {
    MavenFacet facet = repository.facet(MavenFacet.class);

    if (VersionPolicy.SNAPSHOT.equals(facet.getVersionPolicy())) {
      throw new ValidationErrorsException("Upload to snapshot repositories not supported, use the maven client.");
    }

    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    return TransactionalStoreBlob.operation.withDb(storageFacet.txSupplier()).throwing(IOException.class).call(() -> {
      Optional<AssetUpload> pomAsset = findPomAsset(componentUpload);
      if (pomAsset.isPresent()) {
        PartPayload payload = pomAsset.get().getPayload();

        try (TempBlob pom = storageFacet.createTempBlob(payload, HashType.ALGORITHMS)) {
          pomAsset.get().setPayload(new TempBlobPartPayload(payload, pom));
          return createAssets(repository.getName(), facet, createBasePathFromPom(pom),
              componentUpload.getAssetUploads());
        }
      }
      else {
        Map<String, String> componentFields = componentUpload.getFields();
        String basePath = createBasePath(componentFields.get(GROUP_ID), componentFields.get(ARTIFACT_ID),
            componentFields.get(VERSION));

        List<String> assetPaths = createAssets(repository.getName(), facet, basePath,
            componentUpload.getAssetUploads());

        if (Boolean.valueOf(componentUpload.getField(GENERATE_POM))) {
          String pomPath = generatePom(facet, basePath, componentFields.get(GROUP_ID),
              componentFields.get(ARTIFACT_ID), componentFields.get(VERSION), componentFields.get(PACKAGING));

          assetPaths.add(pomPath);
        }

        return assetPaths;
      }
    });
  }

  private List<String> createAssets(final String repositoryName,
                                    final MavenFacet facet,
                                    final String basePath,
                                    final List<AssetUpload> assetUploads)
      throws IOException
  {
    List<String> assets = new ArrayList<>();

    for (AssetUpload asset : assetUploads) {
      StringBuilder path = new StringBuilder(basePath);

      String classifier = asset.getFields().get(CLASSIFIER);
      if (!Strings2.isEmpty(classifier)) {
        path.append('-').append(classifier);
      }
      path.append('.').append(asset.getFields().get(EXTENSION));

      MavenPath mavenPath = parser.parsePath(path.toString());

      if (!versionPolicyValidator.validArtifactPath(facet.getVersionPolicy(), mavenPath.getCoordinates())) {
        throw new ValidationErrorsException(
            format("Version policy mismatch, cannot upload %s content to %s repositories for file '%s'",
                facet.getVersionPolicy().equals(VersionPolicy.RELEASE) ? VersionPolicy.SNAPSHOT.name() : VersionPolicy.RELEASE.name(),
                facet.getVersionPolicy().name(),
                assets.size()));
      }

      ensurePermitted(repositoryName, Maven2Format.NAME, mavenPath.getPath(), toMap(mavenPath.getCoordinates()));

      facet.put(mavenPath, asset.getPayload());
      assets.add(mavenPath.getPath());
    }

    return assets;
  }

  private String createBasePath(final String groupId, final String artifactId, final String version) {
    List<String> parts = newArrayList(groupId.split("\\."));
    parts.addAll(Arrays.asList(artifactId, version, artifactId));
    return join("-", join("/", parts), version);
  }

  private String createBasePathFromPom(final TempBlob tempBlob) throws IOException {
    try (InputStream in = tempBlob.get()) {
      Model model = MavenModels.readModel(in);
      validatePom(model);
      return createBasePath(getGroupId(model), getArtifactId(model), getVersion(model));
    }
  }

  private String generatePom(final MavenFacet mavenFacet,
                             final String basePath,
                             final String groupId,
                             final String artifactId,
                             final String version,
                             @Nullable final String packaging)
      throws IOException
  {
    log.debug("Generating pom for {} {} {} with packaging {}", groupId, artifactId, version, packaging);

    String pom = mavenPomGenerator.generatePom(groupId, artifactId, version, packaging);

    MavenPath mavenPath = parser.parsePath(basePath + ".pom");
    mavenFacet.put(mavenPath, new StringPayload(pom, "text/xml"));
    return mavenPath.getPath();
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> componentFields = Arrays.asList(
          new UploadFieldDefinition(GROUP_ID, GROUP_ID_DISPLAY, null, false, Type.STRING),
          new UploadFieldDefinition(ARTIFACT_ID, ARTIFACT_ID_DISPLAY, null, false, Type.STRING),
          new UploadFieldDefinition(VERSION, false, Type.STRING),
          new UploadFieldDefinition(GENERATE_POM, GENERATE_POM_DISPLAY, null, true, Type.BOOLEAN),
          new UploadFieldDefinition(PACKAGING, true, Type.STRING));

      List<UploadFieldDefinition> assetFields = Arrays.asList(new UploadFieldDefinition(CLASSIFIER, true, Type.STRING),
          new UploadFieldDefinition(EXTENSION, false, Type.STRING));

      UploadRegexMap regexMap = new UploadRegexMap(
          "-(?:(?:\\.?\\d)+)(?:-(?:SNAPSHOT|\\d+))?(?:-(\\w+))?\\.((?:\\.?\\w)+)$", CLASSIFIER, EXTENSION);

      definition = new UploadDefinition(Maven2Format.NAME, true, componentFields, assetFields, regexMap);
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }

  private Map<String, String> toMap(final Coordinates coordinates) {
    Map<String, String> map = new HashMap<>();
    map.put("groupId", coordinates.getGroupId());
    map.put("artifactId", coordinates.getArtifactId());
    map.put("version", coordinates.getVersion());
    if (coordinates.getClassifier() != null) {
      map.put("classifier", coordinates.getClassifier());
    }
    if (coordinates.getExtension() != null) {
      map.put("extension", coordinates.getExtension());
    }
    return map;
  }

  @Override
  public void validate(final ComponentUpload componentUpload) {
    ValidationErrorsException exception = new ValidationErrorsException();

    if (componentUpload.getAssetUploads().isEmpty()) {
      exception.withError("No assets found in upload");
    }

    AtomicInteger assetCounter = new AtomicInteger();
    componentUpload.getAssetUploads().stream()
        .forEachOrdered(asset -> {
          int assetCount = assetCounter.incrementAndGet();

          if (asset.getPayload() == null) {
            exception.withError("file", format("Missing file on asset '%s'", assetCount));
          }

          getDefinition().getAssetFields().stream()
              .filter(field -> !field.isOptional())
              .filter(field -> isBlank(asset.getField(field.getName())))
              .forEach(field -> exception.withError(field.getName(),
                  format("Missing required asset field '%s' on '%s'", field.getDisplayName(), assetCount)));
        });

    Optional<AssetUpload> pomAsset = findPomAsset(componentUpload);
    if (!pomAsset.isPresent()) {
      getDefinition().getComponentFields().stream()
          .filter(field -> !field.isOptional())
          .filter(field -> isBlank(componentUpload.getField(field.getName())))
          .forEach(field -> exception.withError(field.getName(),
              format("Missing required component field '%s'", field.getDisplayName())));

    }

    if (!exception.getValidationErrors().isEmpty()) {
      throw exception;
    }
  }

  private Optional<AssetUpload> findPomAsset(final ComponentUpload componentUpload) {
    return componentUpload.getAssetUploads().stream()
        .filter(asset -> "pom".equals(asset.getField(EXTENSION)) && isBlank(asset.getField(CLASSIFIER)))
        .findFirst();
  }

  @VisibleForTesting
  void validatePom(final Model model) {
    if (model == null) {
      throw new ValidationErrorsException("The provided POM file is invalid.");
    }

    String groupId = getGroupId(model);
    String version = getVersion(model);
    String artifactId = getArtifactId(model);

    if (groupId == null || artifactId == null || version == null ||
        groupId.startsWith(MAVEN_POM_PROPERTY_PREFIX) ||
        artifactId.startsWith(MAVEN_POM_PROPERTY_PREFIX) ||
        version.startsWith(MAVEN_POM_PROPERTY_PREFIX)) {
      throw new ValidationErrorsException(
          format("The provided POM file is invalid.  Could not retrieve valid G:A:V parameters (%s:%s:%s)", groupId,
              artifactId, version));
    }
  }

  private String getGroupId(Model model) {
    String groupId = model.getGroupId();
    if (groupId == null && model.getParent() != null) {
      groupId = model.getParent().getGroupId();
    }
    return groupId;
  }

  private String getArtifactId(Model model) {
    return model.getArtifactId();
  }

  private String getVersion(Model model) {
    String version = model.getVersion();
    if (version == null && model.getParent() != null) {
      version = model.getParent().getVersion();
    }
    return version;
  }
}
