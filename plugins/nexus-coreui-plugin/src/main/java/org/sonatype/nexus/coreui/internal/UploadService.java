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
package org.sonatype.nexus.coreui.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.ComponentUploadExtension;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.upload.WithUploadField;
import org.sonatype.nexus.repository.view.PartPayload;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import org.apache.commons.fileupload.FileItem;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.7
 */
@Named
@Singleton
public class UploadService
  extends ComponentSupport
{
  private UploadManager uploadManager;

  private RepositoryManager repositoryManager;

  private final Set<ComponentUploadExtension> componentUploadExtensions;

  @Inject
  public UploadService(final RepositoryManager repositoryManager,
                       final UploadManager uploadManager,
                       final Set<ComponentUploadExtension> componentsUploadExtensions)
  {
    this.uploadManager = checkNotNull(uploadManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.componentUploadExtensions = checkNotNull(componentsUploadExtensions);
  }

  /**
   * Get a list of available definitions for upload.
   */
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadManager.getAvailableDefinitions();
  }

  /**
   * Perform an upload of assets
   *
   * @param params the form parameters
   * @param files the files being uploaded
   * @return the query term for results in search (depending on upload could show additional results)
   * @throws IOException
   */
  public String upload(final Map<String, String> params, final Map<String, FileItem> files)
      throws IOException
  {
    checkNotNull(params);
    checkNotNull(files);

    String repositoryName = checkNotNull(params.get("repositoryName"), "Missing repositoryName parameter");

    Repository repository = checkNotNull(repositoryManager.get(repositoryName), "Specified repository is missing");

    ComponentUpload componentUpload = createAndValidate(repository, params, files);

    for (ComponentUploadExtension componentUploadExtension : componentUploadExtensions) {
      componentUploadExtension.validate(componentUpload);
    }

    UploadResponse uploadResponse = uploadManager.handle(repository, componentUpload);

    for (ComponentUploadExtension componentUploadExtension : componentUploadExtensions) {
      componentUploadExtension.apply(repository, componentUpload, uploadResponse.getComponentIds());
    }

    return createSearchTerm(uploadResponse.getAssetPaths());
  }

  private ComponentUpload createAndValidate(final Repository repository,
                                            final Map<String, String> params,
                                            final Map<String, FileItem> files)
  {
    ComponentUpload uc = new ComponentUpload();
    UploadDefinition ud = uploadManager.getByFormat(repository.getFormat().toString());

    // create component fields
    createFields(uc, ud.getComponentFields(), "", params);

    // create assets
    for (Entry<String, FileItem> file : files.entrySet()) {
      String suffix = file.getKey().substring("file".length());
      AssetUpload ua = new AssetUpload();

      createFields(ua, ud.getAssetFields(), suffix, params);
      final FileItem fileItem = file.getValue();
      ua.setPayload(new FileItemPayload(fileItem));

      uc.getAssetUploads().add(ua);
    }

    logUploadDetails(uc, repository.getName(), repository.getFormat().getValue());

    return uc;
  }

  private void createFields(final WithUploadField item,
                            final List<UploadFieldDefinition> fields,
                            final String suffix,
                            final Map<String, String> params)
  {
    for (UploadFieldDefinition assetField : fields) {
      String formField = assetField.getName() + suffix;
      String value = params.get(formField);
      if (!Strings2.isEmpty(value)) {
        if (Type.BOOLEAN.equals(assetField.getType())) {
          item.getFields().put(assetField.getName(), "on".equals(value) ? "true" : "false");
        }
        else {
          item.getFields().put(assetField.getName(), value);
        }
      }
    }
  }

  private void logUploadDetails(ComponentUpload componentUpload, String repository, String format) {
    Map<String,String> componentFields = componentUpload.getFields();
    List<AssetUpload> assetUploads = componentUpload.getAssetUploads();

    StringBuilder sb = new StringBuilder();
    sb.append("Uploading component with parameters: ")
        .append("repository").append("=\"").append(repository).append("\" ")
        .append("format").append("=\"").append(format).append("\" ");
    for (Entry<String,String> entry : componentFields.entrySet()) {
      sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
    }
    log.info(sb.toString());

    for (AssetUpload assetUpload : assetUploads) {
      sb = new StringBuilder();
      sb.append("Asset with parameters: ");
      sb.append("file=\"").append(assetUpload.getPayload().getName()).append("\" ");
      for (Entry<String,String> entry : assetUpload.getFields().entrySet()) {
        sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
      }
      log.info(sb.toString());
    }
  }

  @VisibleForTesting
  String createSearchTerm(final Collection<String> createdPaths) {
    if (createdPaths.isEmpty()) {
      return null;
    }

    String prefix = Iterables.getFirst(createdPaths, null);

    for (String path : createdPaths) {
      prefix = longestPrefix(prefix, path);
    }

    return elasticEscape(prefix);
  }

  private String removeLastSegment(final String path) {
    int index = path.lastIndexOf('/');
    if (index != -1) {
      return path.substring(0, index);
    }
    return path;
  }

  private String elasticEscape(final String query) {
    return query.replace("/", "\\/").replace(".", "\\.").replace("-", "\\-");
  }

  private String longestPrefix(final String prefix, final String path) {
    String result = prefix;
    while (result.length() > 0 && !path.startsWith(result)) {
      result = removeLastSegment(result);
    }
    return result;
  }

  private static class FileItemPayload implements PartPayload
  {
    private final FileItem fileItem;

    FileItemPayload(final FileItem fileItem) {
      this.fileItem = fileItem;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return fileItem.getInputStream();
    }

    @Override
    public long getSize() {
      return fileItem.getSize();
    }

    @Override
    public String getContentType() {
      return fileItem.getContentType();
    }

    @Override
    public String getName() {
      return fileItem.getName();
    }

    @Override
    public String getFieldName() {
      return fileItem.getFieldName();
    }

    @Override
    public boolean isFormField() {
      return true;
    }

  }
}
