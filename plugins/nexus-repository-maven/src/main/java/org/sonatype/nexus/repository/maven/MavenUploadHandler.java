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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.MavenFacetUtils;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.maven.internal.MavenPomGenerator;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.upload.ValidatingComponentUpload;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import org.apache.maven.model.Model;
import org.joda.time.DateTime;

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
    extends UploadHandlerSupport
{
  private static final String COMPONENT_COORDINATES_GROUP = "Component coordinates";

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
                            final MavenPomGenerator mavenPomGenerator,
                            final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.parser = parser;
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
    this.versionPolicyValidator = versionPolicyValidator;
    this.mavenPomGenerator = mavenPomGenerator;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    checkNotNull(repository);
    checkNotNull(upload);

    return doUpload(repository, upload);
  }

  private UploadResponse doUpload(final Repository repository, final ComponentUpload componentUpload) throws IOException {
    MavenFacet facet = repository.facet(MavenFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    if (VersionPolicy.SNAPSHOT.equals(facet.getVersionPolicy())) {
      throw new ValidationErrorsException("Upload to snapshot repositories not supported, use the maven client.");
    }

    ContentAndAssetPathResponseData responseData;

    AssetUpload pomAsset = findPomAsset(componentUpload);

    //purposefully not using a try with resources, as this will only be used in case of an included pom file, which
    //isn't required
    TempBlob pom = null;

    try {
      if (pomAsset != null) {
        PartPayload payload = pomAsset.getPayload();
        pom = storageFacet.createTempBlob(payload, HashType.ALGORITHMS);
        pomAsset.setPayload(new TempBlobPartPayload(payload, pom));
      }

      String basePath = getBasePath(componentUpload, pom);

      doValidation(repository, basePath, componentUpload.getAssetUploads());

      UnitOfWork.begin(storageFacet.txSupplier());
      try {
        responseData = createAssets(repository, basePath, componentUpload.getAssetUploads());

        if (isGeneratePom(componentUpload.getField(GENERATE_POM))) {
          String pomPath = generatePom(repository, basePath, componentUpload.getFields().get(GROUP_ID),
              componentUpload.getFields().get(ARTIFACT_ID), componentUpload.getFields().get(VERSION),
              componentUpload.getFields().get(PACKAGING));

          responseData.addAssetPath(pomPath);
        }

        updateMetadata(repository, responseData.coordinates);
      }
      finally {
        UnitOfWork.end();
      }

      return new UploadResponse(responseData.getContent(), responseData.getAssetPaths());
    }
    finally {
      if (pom != null) {
        pom.close();
      }
    }
  }

  private String getBasePath(final ComponentUpload componentUpload, final TempBlob pom) throws IOException
  {
    if (pom != null) {
      return createBasePathFromPom(pom);
    }

    return createBasePath(componentUpload.getFields().get(GROUP_ID), componentUpload.getFields().get(ARTIFACT_ID),
        componentUpload.getFields().get(VERSION));
  }

  private void doValidation(final Repository repository,
                               final String basePath,
                               final List<AssetUpload> assetUploads)
  {
    for (int i = 0 ; i < assetUploads.size() ; i++) {
      AssetUpload asset = assetUploads.get(i);
      StringBuilder path = new StringBuilder(basePath);

      String classifier = asset.getFields().get(CLASSIFIER);
      if (!Strings2.isEmpty(classifier)) {
        path.append('-').append(classifier);
      }
      path.append('.').append(asset.getFields().get(EXTENSION));

      MavenPath mavenPath = parser.parsePath(path.toString());

      if (mavenPath.getCoordinates() == null) {
        throw new ValidationErrorsException(
            format("Cannot generate maven coordinate from assembled path '%s'", mavenPath.getPath()));
      }

      MavenFacet facet = repository.facet(MavenFacet.class);

      if (!versionPolicyValidator
          .validArtifactPath(facet.getVersionPolicy(), mavenPath.getCoordinates())) {
        throw new ValidationErrorsException(
            format("Version policy mismatch, cannot upload %s content to %s repositories for file '%s'",
                facet.getVersionPolicy().equals(VersionPolicy.RELEASE) ? VersionPolicy.SNAPSHOT.name() : VersionPolicy.RELEASE.name(),
                facet.getVersionPolicy().name(),
                i));
      }

      ensurePermitted(repository.getName(), Maven2Format.NAME, mavenPath.getPath(), toMap(mavenPath.getCoordinates()));
    }
  }

  private ContentAndAssetPathResponseData createAssets(final Repository repository,
                                                       final String basePath,
                                                       final List<AssetUpload> assetUploads)
      throws IOException
  {
    ContentAndAssetPathResponseData responseData = new ContentAndAssetPathResponseData();

    for (AssetUpload asset : assetUploads) {
      StringBuilder path = new StringBuilder(basePath);

      String classifier = asset.getFields().get(CLASSIFIER);
      if (!Strings2.isEmpty(classifier)) {
        path.append('-').append(classifier);
      }
      path.append('.').append(asset.getFields().get(EXTENSION));

      MavenPath mavenPath = parser.parsePath(path.toString());

      Content content = storeAssetContent(repository, mavenPath, asset.getPayload());

      //We only need to set the component id one time
      if(responseData.getContent() == null) {
        responseData.setContent(content);
      }
      responseData.addAssetPath(mavenPath.getPath());

      //All assets belong to same component, so just grab the coordinates for one of them
      if (responseData.getCoordinates() == null) {
        responseData.setCoordinates(mavenPath.getCoordinates());
      }
    }

    return responseData;
  }

  protected Content storeAssetContent(final Repository repository,
                                      final MavenPath mavenPath,
                                      final Payload payload) throws IOException
  {
    MavenFacet mavenFacet = repository.facet(MavenFacet.class);
    Content content = mavenFacet.put(mavenPath, payload);
    putChecksumFiles(mavenFacet, mavenPath, content);

    return content;
  }

  private void putChecksumFiles(final MavenFacet facet, final MavenPath path, final Content content) throws IOException {
    DateTime dateTime = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
    Map<HashAlgorithm, HashCode> hashes = MavenFacetUtils.getHashAlgorithmFromContent(content.getAttributes());
    MavenFacetUtils.addHashes(facet, path, hashes, dateTime);
  }

  private void updateMetadata(final Repository repository, final Coordinates coordinates) {
    if (coordinates != null) {
      repository.facet(MavenHostedFacet.class)
          .rebuildMetadata(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), false);
    }
    else {
      log.debug("Not updating metadata.xml files since coordinate could not be retrieved from path");
    }
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

  private String generatePom(final Repository repository,
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

    storeAssetContent(repository, mavenPath, new StringPayload(pom, "text/xml"));

    return mavenPath.getPath();
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> componentFields = Arrays.asList(
          new UploadFieldDefinition(GROUP_ID, GROUP_ID_DISPLAY, null, false, Type.STRING, COMPONENT_COORDINATES_GROUP),
          new UploadFieldDefinition(ARTIFACT_ID, ARTIFACT_ID_DISPLAY, null, false, Type.STRING, COMPONENT_COORDINATES_GROUP),
          new UploadFieldDefinition(VERSION, false, Type.STRING, COMPONENT_COORDINATES_GROUP),
          new UploadFieldDefinition(GENERATE_POM, GENERATE_POM_DISPLAY, null, true, Type.BOOLEAN, COMPONENT_COORDINATES_GROUP),
          new UploadFieldDefinition(PACKAGING, true, Type.STRING, COMPONENT_COORDINATES_GROUP));

      List<UploadFieldDefinition> assetFields = Arrays.asList(
          new UploadFieldDefinition(CLASSIFIER, true, Type.STRING),
          new UploadFieldDefinition(EXTENSION, false, Type.STRING));

      UploadRegexMap regexMap = new UploadRegexMap(
          "-(?:(?:\\.?\\d)+)(?:-(?:SNAPSHOT|\\d+))?(?:-(\\w+))?\\.((?:\\.?\\w)+)$", CLASSIFIER, EXTENSION);

      definition = getDefinition(Maven2Format.NAME, true, componentFields, assetFields, regexMap);
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
  public ValidatingComponentUpload getValidatingComponentUpload(final ComponentUpload componentUpload) {
    return new MavenValidatingComponentUpload(getDefinition(), componentUpload);
  }

  private AssetUpload findPomAsset(final ComponentUpload componentUpload) {
    return componentUpload.getAssetUploads().stream()
        .filter(asset -> "pom".equals(asset.getField(EXTENSION)) && isBlank(asset.getField(CLASSIFIER)))
        .findFirst().orElse(null);
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

  private static boolean isGeneratePom(final String generatePom) {
    return generatePom != null && ("on".equals(generatePom) || Boolean.valueOf(generatePom));
  }

  /**
   * Simple data carrier used to collect data needed
   * to populate the {@link UploadResponse}
   */
  private static class ContentAndAssetPathResponseData {
    Content content;
    List<String> assetPaths = newArrayList();
    Coordinates coordinates;

    public void setContent(final Content content) {
      this.content = content;
    }

    public void setCoordinates(final Coordinates coordinates) {
      this.coordinates = coordinates;
    }

    public void addAssetPath(final String assetPath) {
      this.assetPaths.add(assetPath);
    }

    public Content getContent() {return this.content;}

    public Coordinates getCoordinates() {
      return this.coordinates;
    }

    public List<String> getAssetPaths() { return this.assetPaths;}

    public UploadResponse uploadResponse() {
      return new UploadResponse(content, assetPaths);
    }
  }
}
