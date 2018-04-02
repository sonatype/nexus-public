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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Parameters;

import com.google.common.base.Strings;

/**
 * Extractor object allowing easy extraction of information from a {@code Parameters} object as they pertain to npm
 * V1 search, including substitution of sensible defaults.
 *
 * @since 3.7
 */
@Named
@Singleton
public class NpmSearchParameterExtractor
    extends ComponentSupport
{
  private static final String WILDCARD = "*";

  /**
   * Extracts the text parameter from the npm search request, performing the necessary rewrites and substitutions in an
   * ad hoc manner so that queries on specific fields are supported.
   */
  public String extractText(final Parameters parameters) {
    String textParameter = Strings.nullToEmpty(parameters.get("text")).trim();
    if (!textParameter.isEmpty()) {
      // HACK: replace the various special search fields with their ES field names, and assume "maintainer" is author
      // A better way would be to parse the actual query into an AST and then rewrite, but we're not doing that for now
      textParameter = textParameter
          .replaceAll("keywords:", "assets.attributes.npm.keywords:")
          .replaceAll("author:", "assets.attributes.npm.author:")
          .replaceAll("maintainer:", "assets.attributes.npm.author:")
          .replaceAll("is:", "assets.attributes.npm.tagged_is:")
          .replaceAll("not:", "assets.attributes.npm.tagged_not:")
          .replaceAll("scope:", "assets.attributes.npm.scope:");

      // HACK: if we don't have a keyword that we're specifically searching on, prefix and suffix the search string with
      // wildcard characters to mimic the partial matching behavior that the upstream npm search endpoint already does
      if (!textParameter.contains(":")) {
        textParameter = WILDCARD + textParameter + WILDCARD;
      }
    }
    return textParameter;
  }

  /**
   * Extracts the size parameter, using a default of 20 when not supplied and 250 as a maximum value (following the
   * npm search V1 documentation as of this writing).
   */
  public int extractSize(final Parameters parameters) {
    int size = 20;
    String sizeParameter = Strings.nullToEmpty(parameters.get("size")).trim();
    if (!sizeParameter.isEmpty()) {
      try {
        size = Integer.parseInt(sizeParameter);
      }
      catch (NumberFormatException e) {
        log.debug("Invalid size encountered in search parameters {}, using default", parameters, e);
      }
      if (size < 1) {
        size = 1;
      }
      else if (size > 250) {
        size = 250;
      }
    }
    return size;
  }

  /**
   * Extracts the {@code from} parameter, assuming that the results returned should start at zero when not otherwise
   * specified.
   */
  public int extractFrom(final Parameters parameters) {
    int from = 0;
    String fromParameter = Strings.nullToEmpty(parameters.get("from")).trim();
    if (!fromParameter.isEmpty()) {
      try {
        from = Integer.parseInt(fromParameter);
      }
      catch (NumberFormatException e) {
        log.debug("Invalid from encountered in search parameters {}, using default", parameters, e);
      }
      if (from < 0) {
        from = 0;
      }
    }
    return from;
  }
}
