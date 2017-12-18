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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandler;

import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenUploadHandler
    implements UploadHandler
{
  private static final String EXTENSION = "extension";

  private static final String CLASSIFIER = "classifier";

  private static final String VERSION = "version";

  private static final String ARTIFACT_ID = "artifactId";

  private static final String GROUP_ID = "groupId";

  private static final String ARTIFACT_ID_DISPLAY = "Artifact ID";

  private static final String GROUP_ID_DISPLAY = "Group ID";

  private final Maven2MavenPathParser parser;

  private UploadDefinition definition;

  @Inject
  public MavenUploadHandler(final Maven2MavenPathParser parser) {
    this.parser = parser;
  }

  @Override
  public Collection<String> handle(final Repository repository, final ComponentUpload upload) throws IOException {
    checkNotNull(repository);
    checkNotNull(upload);

    return doUpload(repository, upload);
  }

  private List<String> doUpload(final Repository repository, final ComponentUpload component) throws IOException {
    MavenFacet facet = repository.facet(MavenFacet.class);
    Map<String, String> componentFields = checkNotNull(component.getFields());

    String artifactId = componentFields.get(ARTIFACT_ID);
    String version = componentFields.get(VERSION);

    StringBuilder basePath = new StringBuilder();
    Joiner.on('/').appendTo(basePath, componentFields.get(GROUP_ID).split("\\."));
    basePath.append('/');
    Joiner.on('/').appendTo(basePath, artifactId, version, artifactId);
    basePath.append('-').append(version);

    List<String> assets = new ArrayList<>();

    TransactionalStoreBlob.operation.withDb(repository.facet(StorageFacet.class).txSupplier())
        .throwing(IOException.class).run(() -> {
          for (AssetUpload asset : component.getAssetUploads()) {
            StringBuilder path = new StringBuilder(basePath);

            String classifier = asset.getFields().get(CLASSIFIER);
            if (!Strings2.isEmpty(classifier)) {
              path.append('-').append(classifier);
            }
            path.append('.').append(asset.getFields().get(EXTENSION));

            MavenPath mavenPath = parser.parsePath(path.toString());
            facet.put(mavenPath, asset.getPayload());
            assets.add(mavenPath.getPath());
          }
        });
    return assets;
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> componentFields = Arrays.asList(
          new UploadFieldDefinition(GROUP_ID, GROUP_ID_DISPLAY, false, Type.STRING),
          new UploadFieldDefinition(ARTIFACT_ID, ARTIFACT_ID_DISPLAY, false, Type.STRING),
          new UploadFieldDefinition(VERSION, false, Type.STRING));

      List<UploadFieldDefinition> assetFields = Arrays.asList(new UploadFieldDefinition(CLASSIFIER, true, Type.STRING),
          new UploadFieldDefinition(EXTENSION, false, Type.STRING));

      definition = new UploadDefinition(Maven2Format.NAME, true, componentFields, assetFields);
    }
    return definition;
  }
}
