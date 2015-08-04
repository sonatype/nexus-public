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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since yum 3.0
 */
public class AliasMappingsValidator
    implements Validator
{

  private final String key;

  public AliasMappingsValidator(final String key) {
    this.key = checkNotNull(key);
  }

  @Override
  public ValidationResult validate(final Map<String, String> properties) {
    try {
      new AliasMappings(properties.get(key));
      return ValidationResult.VALID;
    }
    catch (IllegalArgumentException e) {
      return new DefaultValidationResult().add(key, e.getMessage());
    }
  }

  @Override
  public String explainValid() {
    return "Valid alias mappings";
  }

  @Override
  public String explainInvalid() {
    return "Invalid alias mappings. Expected <alias>=<version> entries separated by comma (,)";
  }

}
