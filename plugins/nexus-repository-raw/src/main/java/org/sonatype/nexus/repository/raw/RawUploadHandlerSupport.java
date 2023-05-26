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
package org.sonatype.nexus.repository.raw;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

/**
 * Common base for raw upload handlers
 *
 * @since 3.24
 */
public abstract class RawUploadHandlerSupport
    extends UploadHandlerSupport
{
  protected static final String FILENAME = "filename";

  protected static final String DIRECTORY = "directory";

  protected static final String DIRECTORY_HELP_TEXT = "Destination for uploaded files (e.g. /path/to/files/)";

  protected static final String FIELD_GROUP_NAME = "Component attributes";

  protected final ContentPermissionChecker contentPermissionChecker;

  protected final VariableResolverAdapter variableResolverAdapter;

  protected final boolean datastoreEnabled;

  protected UploadDefinition definition;

  public RawUploadHandlerSupport(
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
      final boolean datastoreEnabled)
  {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = variableResolverAdapter;
    this.datastoreEnabled = datastoreEnabled;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    String basePath = upload.getFields().get(DIRECTORY).trim();

    //Data holders for populating the UploadResponse
    Map<String,PartPayload> pathToPayload = new LinkedHashMap<>();

    for (AssetUpload asset : upload.getAssetUploads()) {
      String path = normalizePath(basePath + '/' + asset.getFields().get(FILENAME).trim());

      String pathWithPrefix = datastoreEnabled ? prependIfMissing(path, "/") : path;
      ensurePermitted(repository.getName(), RawFormat.NAME, pathWithPrefix, emptyMap());

      pathToPayload.put(path, asset.getPayload());
    }

    List<Content> responseContents = getResponseContents(repository, pathToPayload);

    return new UploadResponse(responseContents, new ArrayList<>(pathToPayload.keySet()));
  }

  protected abstract List<Content> getResponseContents(final Repository repository,
                                                       final Map<String, PartPayload> pathToPayload)
      throws IOException;

  @Override
  public Content handle(
      final Repository repository,
      final File content,
      final String path)
      throws IOException
  {
    // TODO: Remove this handler once all formats have been converted to work with ImportFileConfiguration
    return handle(new ImportFileConfiguration(repository, content, path));
  }

  @Override
  public Content handle(final ImportFileConfiguration configuration) throws IOException {

    ensurePermitted(configuration.getRepository().getName(), RawFormat.NAME, configuration.getAssetName(), emptyMap());

    return doPut(configuration);
  }

  protected abstract Content doPut(final ImportFileConfiguration configuration) throws IOException;

  protected String normalizePath(final String path) {
    String result = path.replaceAll("/+", "/");

    if (result.startsWith("/")) {
      result = result.substring(1);
    }

    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }

    return result;
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(RawFormat.NAME, true,
          singletonList(new UploadFieldDefinition(DIRECTORY, DIRECTORY_HELP_TEXT, false, Type.STRING, FIELD_GROUP_NAME)),
          singletonList(new UploadFieldDefinition(FILENAME, false, Type.STRING)),
          new UploadRegexMap("(.*)", FILENAME));
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
