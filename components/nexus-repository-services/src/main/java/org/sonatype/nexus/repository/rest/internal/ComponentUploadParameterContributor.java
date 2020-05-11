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
package org.sonatype.nexus.repository.rest.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.swagger.ParameterContributor;

import com.google.common.collect.ImmutableList;
import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.FormParameter;

import static io.swagger.models.HttpMethod.POST;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.8
 */
@Named
@Singleton
public class ComponentUploadParameterContributor
    extends ParameterContributor<FormParameter>
{
  private static final List<HttpMethod> HTTP_METHODS = ImmutableList.of(POST);

  private static final List<String> PATHS = ImmutableList.of(V1_API_PREFIX + "/components");

  @Inject
  public ComponentUploadParameterContributor(final UploadManager uploadManager) {
    super(HTTP_METHODS, PATHS, transformUploadDefinitions(uploadManager.getAvailableDefinitions()));
  }

  private static Collection<FormParameter> transformUploadDefinitions(final Collection<UploadDefinition> uploadDefinitions) {
    Collection<FormParameter> parameters = new ArrayList<>();

    for (UploadDefinition uploadDefinition : uploadDefinitions) {
      uploadDefinition.getComponentFields().forEach(uploadFieldDefinition -> parameters.add(new FormParameter()
          .name(uploadDefinition.getFormat() + "." + uploadFieldDefinition.getName())
          .type(uploadFieldDefinition.getType().name().toLowerCase())
          .description(uploadDefinition.getFormat() + " " + uploadFieldDefinition.getDisplayName())));

      for (int i = 1; i <= (uploadDefinition.isMultipleUpload() ? 3 : 1); i++) {
        String assetIndex = uploadDefinition.isMultipleUpload() ? Integer.toString(i) : "";
        String assetName = uploadDefinition.getFormat() + ".asset" + assetIndex;
        String assetDisplayName = uploadDefinition.getFormat() + " Asset " + assetIndex;

        parameters.add(new FormParameter()
            .name(assetName)
            .type("file")
            .description(assetDisplayName));

        uploadDefinition.getAssetFields().forEach(uploadFieldDefinition -> parameters.add(new FormParameter()
            .name(assetName + "." + uploadFieldDefinition.getName())
            .type(uploadFieldDefinition.getType().name().toLowerCase())
            .description(assetDisplayName + " " + uploadFieldDefinition.getDisplayName())));
      }
    }

    return parameters;
  }
}
