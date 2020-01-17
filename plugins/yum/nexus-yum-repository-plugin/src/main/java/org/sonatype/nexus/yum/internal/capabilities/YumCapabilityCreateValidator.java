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

import org.sonatype.nexus.plugins.capabilities.ValidationResult;
import org.sonatype.nexus.plugins.capabilities.Validator;
import org.sonatype.nexus.plugins.capabilities.support.validator.DefaultValidationResult;

import static org.sonatype.nexus.yum.YumRegistry.DEFAULT_CREATEREPO_PATH;
import static org.sonatype.nexus.yum.YumRegistry.DEFAULT_MERGEREPO_PATH;
import static org.sonatype.nexus.yum.internal.capabilities.YumCapabilityConfiguration.CREATEREPO_PATH;
import static org.sonatype.nexus.yum.internal.capabilities.YumCapabilityConfiguration.MERGEREPO_PATH;

public class YumCapabilityCreateValidator
    implements Validator
{
  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    String createrepoPath = properties.get(CREATEREPO_PATH);
    String mergerepoPath = properties.get(MERGEREPO_PATH);

    DefaultValidationResult result = new DefaultValidationResult();

    if (!DEFAULT_CREATEREPO_PATH.equals(createrepoPath)) {
      result.add(String.format("%s must be '%s', you can set it later in sonatype-work/nexus/conf/capabilities.xml",
          CREATEREPO_PATH,
          DEFAULT_CREATEREPO_PATH));
    }

    if (!DEFAULT_MERGEREPO_PATH.equals(mergerepoPath)) {
      result.add(String.format("%s must be '%s', you can set it later in sonatype-work/nexus/conf/capabilities.xml",
          MERGEREPO_PATH,
          DEFAULT_MERGEREPO_PATH));
    }

    return result;
  }

  @Override
  public String explainValid() {
    return "Valid createrepo and mergerepo paths";
  }

  @Override
  public String explainInvalid() {
    return String
        .format("Invalid createrepo or mergerepo paths. Expected default values of %s and %s",
            DEFAULT_CREATEREPO_PATH,
            DEFAULT_MERGEREPO_PATH);
  }
}
