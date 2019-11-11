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
package org.sonatype.nexus.yum.internal.capabilities;

import java.util.Map;
import java.util.Objects;

import javax.ws.rs.core.Response.Status;

import org.sonatype.nexus.plugins.capabilities.CapabilityContext;
import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

import static org.sonatype.nexus.yum.YumRegistry.DEFAULT_CREATEREPO_PATH;
import static org.sonatype.nexus.yum.YumRegistry.DEFAULT_MERGEREPO_PATH;
import static org.sonatype.nexus.yum.internal.capabilities.YumCapabilityConfiguration.CREATEREPO_PATH;
import static org.sonatype.nexus.yum.internal.capabilities.YumCapabilityConfiguration.MERGEREPO_PATH;

public class YumCapabilityUpdateValidator
    implements Validator
{
  private final Map<String, String> existingProperties;

  YumCapabilityUpdateValidator(final Map<String, String> existingProperties) {
    this.existingProperties = existingProperties;
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    String createrepoPath = properties.get(CREATEREPO_PATH);
    String mergerepoPath = properties.get(MERGEREPO_PATH);

    String existingCreateRepoPath = existingProperties.get(CREATEREPO_PATH);
    String existingMergeRepoPath = existingProperties.get(MERGEREPO_PATH);

    DefaultValidationResult result = new DefaultValidationResult();

    if (!Objects.equals(createrepoPath, existingCreateRepoPath)) {
      result.add(String.format("%s can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml",
          CREATEREPO_PATH));
    }

    if (!Objects.equals(mergerepoPath, existingMergeRepoPath)) {
      result.add(String.format("%s can not be modified, you can change it in sonatype-work/nexus/conf/capabilities.xml",
          MERGEREPO_PATH));
    }

    return result;
  }

  @Override
  public String explainValid() {
    return "Valid createrepo and mergerepo paths";
  }

  @Override
  public String explainInvalid() {
    return "Invalid createrepo or mergerepo paths. Values can not be modified";
  }
}
