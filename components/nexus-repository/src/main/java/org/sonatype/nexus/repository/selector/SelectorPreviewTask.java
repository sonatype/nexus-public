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
package org.sonatype.nexus.repository.selector;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.selector.SelectorPreview.ContentType;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.selector.SelectorPreviewTaskDescriptor.CONTENT_TYPE_FIELD_ID;
import static org.sonatype.nexus.repository.selector.SelectorPreviewTaskDescriptor.SELECTOR_FIELD_ID;

/**
 * A {@link Task} to preview the content matching the configured selector expressions.
 * The matching content will be logged.
 *
 * @since 3.0
 */
@Named
public class SelectorPreviewTask
    extends TaskSupport
{
  private final RepositoryManager repositoryManager;
  private SelectorPreview selectorPreview;

  @Inject
  public SelectorPreviewTask(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public String getMessage() {
    return "Submitting Selector Preview Task";
  }

  @Override
  public void configure(final TaskConfiguration configuration) {
    super.configure(configuration);

    Selector selector = new JexlSelector(configuration.getString(SELECTOR_FIELD_ID));
    selectorPreview = new SelectorPreview(
        repositoryManager,
        ContentType.valueOf(configuration.getString(CONTENT_TYPE_FIELD_ID)),
        selector);
    selectorPreview.eachRepository = r -> log.info("selector: {}, repository: {}", selector, r.getName());
    selectorPreview.eachComponent = r -> c -> log.info("selector: {}, repository: {}, component: {}", selector, r.getName(), c.name());
    selectorPreview.eachAsset = e -> a -> log.info("selector: {}, repository: {}, component: {}, asset: {}", selector, e.getKey().getName(), e.getValue().name(), a.name());
  }

  @Override
  protected Object execute() throws Exception {
    selectorPreview.executePreview();
    return null;
  }
}
