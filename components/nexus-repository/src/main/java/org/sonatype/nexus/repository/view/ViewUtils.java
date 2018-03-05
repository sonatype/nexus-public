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
package org.sonatype.nexus.repository.view;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.apache.http.client.utils.URIBuilder;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.view.Router.LOCAL_ATTRIBUTE_PREFIX;

/**
 * Utility class containing view layer-related functionality not specific to individual classes.
 *
 * @since 3.7
 */
public final class ViewUtils
{
  private ViewUtils() {
    // empty
  }

  /**
   * Builds a url with encoded url parameters, returning the combined url as a string.
   *
   * @throws IllegalArgumentException if the url or its parameters are syntactically invalid.
   */
  public static String buildUrlWithParameters(final String url, final Parameters parameters) {
    checkNotNull(url);
    checkNotNull(parameters);
    try {
      URIBuilder builder = new URIBuilder(url);
      for (Entry<String, String> parameter : parameters.entries()) {
        builder.addParameter(parameter.getKey(), parameter.getValue());
      }
      URI uri = builder.build();
      return uri.toString();
    }
    catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to build url with base url " + url + " and parameters " + parameters,
          e);
    }
  }

  public static void copyLocalContextAttributes(final Context existingContext, final Context newContext) {
    if (existingContext != null) {
      existingContext.getAttributes().keys().stream()
          .filter(key -> !key.startsWith(LOCAL_ATTRIBUTE_PREFIX))
          .forEach(key -> newContext.getAttributes().set(key, existingContext.getAttributes().get(key)));
    }
  }
}
