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
package org.sonatype.nexus.repository.pypi.datastore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.pypi.datastore.internal.PyPiHostedFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.pypi.internal.PyPiAttributes;
import org.sonatype.nexus.repository.pypi.internal.PyPiFileUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.ImmutableMap;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonatype.nexus.common.text.Strings2.isBlank;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_ARCHIVE_TYPE;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;

/**
 * @since 3.next
 */
@Named(PyPiFormat.NAME)
@Singleton
public class PyPiUploadHandler
    extends UploadHandlerSupport
{
  private UploadDefinition definition;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapter variableResolverAdapter;

  @Inject
  public PyPiUploadHandler(
      final ContentPermissionChecker contentPermissionChecker,
      @Named("simple") final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = variableResolverAdapter;
  }

  @Override
  public Content handle(
      final Repository repository, final File content, final String path) throws IOException
  {
    if (content.getName().endsWith(".asc")) {
      return importSignatureFile(content, path, repository);
    }
    else {
      return importPackageFile(content, repository);
    }
  }

  private Content importPackageFile(final File packageFile, final Repository repository) throws IOException {
    PyPiHostedFacet facet = repository.facet(PyPiHostedFacet.class);

    PypiContentFacet contentFacet = repository.facet(PypiContentFacet.class);

    String contentType = Files.probeContentType(packageFile.toPath());
    Payload payload = new StreamPayload(() -> new FileInputStream(packageFile), packageFile.length(), contentType);

    try (TempBlob tempBlob = contentFacet.getTempBlob(payload)) {
      final Map<String, String> metadata = getAndValidateMetadata(repository, tempBlob);

      String name = getAndValidateName(metadata);
      String version = getAndValidateVersion(metadata);
      String filename = packageFile.getName();
      String packagePath = facet.createPackagePath(name, version, filename);

      ensurePermitted(repository.getName(), PyPiFormat.NAME, packagePath, coordinatesFromMetadata(metadata));
      facet.upload(packagePath, metadata, new TempBlobPartPayload("", false, name, contentType, tempBlob));
      return facet.getPackage(packagePath);
    }
  }

  private Content importSignatureFile(final File signatureFile, final String path, final Repository repository)
  {
    PyPiHostedFacet facet = repository.facet(PyPiHostedFacet.class);
    PypiContentFacet contentFacet = repository.facet(PypiContentFacet.class);

    Payload payload = new StreamPayload(() -> new FileInputStream(signatureFile), signatureFile.length(), TEXT_PLAIN);

    try (TempBlob tempBlob = contentFacet.getTempBlob(payload)) {
      String name = PyPiFileUtils.extractNameFromPath(path);
      String version = PyPiFileUtils.extractVersionFromPath(path);

      ensurePermitted(repository.getName(), PyPiFormat.NAME, path, ImmutableMap.of("name", name, "version", version));

      return facet.uploadSignature(name, version,
          new TempBlobPartPayload("", false, signatureFile.getName(), TEXT_PLAIN, tempBlob));
    }
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException
  {
    PyPiHostedFacet facet = repository.facet(PyPiHostedFacet.class);

    PypiContentFacet contentFacet = repository.facet(PypiContentFacet.class);
    PartPayload payload = upload.getAssetUploads().get(0).getPayload();
    try (TempBlob tempBlob = contentFacet.getTempBlob(payload)) {
      final Map<String, String> metadata = getAndValidateMetadata(repository, tempBlob);

      String name = getAndValidateName(metadata);
      String version = getAndValidateVersion(metadata);

      String filename = isNotBlank(payload.getName()) ? payload.getName() :
          name + '-' + version + '.' + metadata.get(P_ARCHIVE_TYPE);

      String path = facet.createPackagePath(name, version, filename);

      ensurePermitted(repository.getName(), PyPiFormat.NAME, path, coordinatesFromMetadata(metadata));

      Content content = facet.upload(filename, metadata, new TempBlobPartPayload(payload, tempBlob)).download();

      return new UploadResponse(Collections.singletonList(content), Collections.singletonList(path));
    }
  }

  private Map<String, String> getAndValidateMetadata(final Repository repository, final TempBlob tempBlob)
      throws IOException
  {
    PyPiHostedFacet facet = repository.facet(PyPiHostedFacet.class);

    final Map<String, String> metadata = facet.extractMetadata(tempBlob);

    if (metadata.isEmpty()) {
      throw new ValidationErrorsException("Unable to extract PyPI metadata from provided archive.");
    }

    return metadata;
  }

  private String getAndValidateName(final Map<String, String> metadata) {
    String name = metadata.get(PyPiAttributes.P_NAME);

    if (isBlank(name)) {
      throw new ValidationErrorsException("Metadata is missing the name attribute");
    }

    return name;
  }

  private String getAndValidateVersion(final Map<String, String> metadata) {
    String version = metadata.get(PyPiAttributes.P_VERSION);

    if (isBlank(version)) {
      throw new ValidationErrorsException("Metadata is missing the version attribute");
    }

    return version;
  }

  private Map<String, String> coordinatesFromMetadata(final Map<String, String> packageMetadata) {
    return ImmutableMap.of(PyPiAttributes.P_NAME, packageMetadata.get(PyPiAttributes.P_NAME), PyPiAttributes.P_VERSION,
        packageMetadata.get(PyPiAttributes.P_VERSION));
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(PyPiFormat.NAME, false);
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

  @Override
  public boolean supportsExportImport() {
    return true;
  }
}
