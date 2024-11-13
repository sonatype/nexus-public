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
package org.sonatype.nexus.cleanup.internal.datastore.search.criteria;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.datastore.search.criteria.AssetCleanupEvaluator;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;

/**
 * Creates an predicate which determines whether an asset's path matches the specified regular expression
 *
 * @since 3.38
 */
@Named(REGEX_KEY)
public class RegexCleanupEvaluator
    extends ComponentSupport
    implements AssetCleanupEvaluator
{
  /*
   * Value is expected to be a regular expression which Java understands.
   */
  @Override
  public Predicate<Asset> getPredicate(final Repository repository, final String value) {
    try {
      Pattern matcher = Pattern.compile(value);
      return asset -> matcher.matcher(asset.path()).matches();
    }
    catch (PatternSyntaxException e) {
      log.error("Repository {} has cleanup policies specifying an invalid regular expression: '{}'",
          repository.getName(), value);

      throw new RuntimeException(
          String.format("Repository %s specifies an invalid regular expression.", repository.getName()), e);
    }
  }
}
