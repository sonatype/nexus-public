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

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.swagger.ParameterContributor;

import com.google.common.collect.ImmutableList;
// NEXUS-46395 spike: OpenAPI 3.x DROPPED FormParameter as a Parameter type. In OpenAPI 3,
// form/multipart inputs are modeled inside a RequestBody's Content.Schema, not as Parameter
// objects. A faithful migration of this contributor needs to switch from extending
// ParameterContributor to a different mechanism that mutates the Operation's RequestBody.
//
// Out of scope for the D1 spike: stubbed below using a generic Parameter so the surrounding
// code compiles and the Phase 3 migration team has a clear marker.
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.Parameter;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;
import org.springframework.stereotype.Component;

/**
 * @since 3.8
 */
@Component
public class ComponentUploadParameterContributor
    extends ParameterContributor<Parameter>
{
  private static final List<HttpMethod> HTTP_METHODS = ImmutableList.of(POST);

  private static final List<String> PATHS = ImmutableList.of(V1_API_PREFIX + "/components");

  @Autowired
  public ComponentUploadParameterContributor(final UploadManager uploadManager) {
    super(HTTP_METHODS, PATHS, transformUploadDefinitions(uploadManager.getAvailableDefinitions()));
  }

  /**
   * NEXUS-46395 SPIKE STUB: returns an empty parameter collection. The proper OpenAPI 3.x
   * design for form data is to mutate the Operation's RequestBody's Content schema rather
   * than push parameters via ParameterContributor. Real Phase 3 work item.
   */
  private static Collection<Parameter> transformUploadDefinitions(
      final Collection<UploadDefinition> uploadDefinitions)
  {
    return new ArrayList<>();
  }
}
