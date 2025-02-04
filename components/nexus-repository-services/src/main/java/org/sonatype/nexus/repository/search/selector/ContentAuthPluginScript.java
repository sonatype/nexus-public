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
package org.sonatype.nexus.repository.search.selector;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.annotations.VisibleForTesting;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.search.lookup.SourceLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.search.index.SearchConstants.FORMAT;
import static org.sonatype.nexus.repository.search.index.SearchConstants.REPOSITORY_NAME;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Native script to work with content selectors from within ES queries.
 *
 * @since 3.1
 */
public class ContentAuthPluginScript
    extends AbstractSearchScript
{
  public static final String NAME = "content_auth";

  private static final Logger log = LoggerFactory.getLogger(ContentAuthPluginScript.class);

  private final Subject subject;

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final ContentPermissionChecker contentPermissionChecker;

  private final RepositoryManager repositoryManager;

  private final boolean contentAuthSleep;

  public ContentAuthPluginScript(
      final Subject subject,
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapterManager variableResolverAdapterManager,
      final RepositoryManager repositoryManager,
      final boolean contentAuthSleep)
  {
    this.subject = checkNotNull(subject);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.contentAuthSleep = contentAuthSleep;
  }

  @Override
  public Object run() {
    ThreadState threadState = new SubjectThreadState(subject);
    threadState.bind();
    try {
      SourceLookup sourceLookup = getSourceLookup();
      String format = (String) checkNotNull(sourceLookup.get(FORMAT));
      String repositoryName = (String) checkNotNull(sourceLookup.get(REPOSITORY_NAME));
      VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> assets =
          (List<Map<String, Object>>) sourceLookup.getOrDefault("assets", Collections.emptyList());
      if (assets != null && !assets.isEmpty()) {
        Map<String, Object> asset = assets.get(0);
        VariableSource variableSource = variableResolverAdapter.fromSourceLookup(sourceLookup, asset);
        Set<String> repoNames = new HashSet<>();
        repoNames.add(repositoryName);
        repoNames.addAll(repositoryManager.findContainingGroups(repositoryName));
        return contentPermissionChecker.isPermitted(repoNames, format, BROWSE, variableSource);
      }
      return false;
    }
    finally {
      threadState.clear();
      if (contentAuthSleep) {
        try {
          TimeUnit.MILLISECONDS.sleep(1);
        }
        catch (InterruptedException e) { // NOSONAR: pooled ES thread
          log.error("Thread.sleep interruped", e);
        }
      }
    }
  }

  /**
   * Delegates to {@link #source()}, only here to aid in unit testing.
   */
  @VisibleForTesting
  protected SourceLookup getSourceLookup() {
    return source();
  }
}
