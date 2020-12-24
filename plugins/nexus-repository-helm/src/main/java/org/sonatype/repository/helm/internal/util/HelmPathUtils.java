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
package org.sonatype.repository.helm.internal.util;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for working with Helm routes and paths.
 *
 * @since 3.28
 */
@Named
@Singleton
public class HelmPathUtils
    extends ComponentSupport
{
  private final IndexYamlAbsoluteUrlRewriter urlRewriter;

  @Inject
  public HelmPathUtils(final IndexYamlAbsoluteUrlRewriter urlRewriter) {
    this.urlRewriter = checkNotNull(urlRewriter);
  }

  /**
   * Returns the filename from a {@link State}.
   */
  public String filename(final State state) {
    return match(state, "filename");
  }

  public String contentFilePath(
      final State state,
      final boolean isForwardingSlash)
  {
    String filename = filename(state);
    return isForwardingSlash ? String.format("/%s", filename) : filename;
  }

  public Optional<String> contentFileUrl(
      final String filename,
      final Content indexYaml)
  {
    return urlRewriter.getFirstUrl(indexYaml, filename);
  }

  public String extension(final State state) {
    return match(state, "extension");
  }

  /**
   * Utility method encapsulating getting a particular token by name from a matcher, including preconditions.
   */
  private String match(final State state, final String name) {
    checkNotNull(state);
    String result = state.getTokens().get(name);
    checkNotNull(result);
    return result;
  }

  /**
   * Returns the {@link State} for the content.
   */
  public State matcherState(final Context context) {
    return context.getAttributes().require(State.class);
  }

  public String buildAssetPath(final State matcherState) {
    String filename = filename(matcherState);
    String extension = extension(matcherState);
    return filename + "." + extension;
  }

  public String buildContentAssetPath(final State matcherState) {
    return String.format("/%s", buildAssetPath(matcherState));
  }
}
