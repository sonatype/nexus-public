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
import org.sonatype.nexus.selector.JexlSelector;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.support.table.ShellTable;

/**
 * Preview what matches a content selector expression.
 *
 * @since 3.0
 */
@Named
@Command(name = "select", scope = "nexus", description = "Preview content selector expression")
public class SelectorPreviewAction
    implements Action
{
  private static final String SELECTOR = "Selector";
  private static final String REPOSITORY_NAME = "Repository Name";
  private static final String COMPONENT_NAME = "Component Name";
  private static final String ASSET_NAME = "Asset Name";

  @Inject
  private RepositoryManager repositoryManager;

  @Argument(index = 0, name = "contentType", description = "type of content being selected", required = true)
  ContentType contentType;

  @Argument(index = 1, name = "expression", description = "repository selector expression")
  JexlSelector selector = new JexlSelector("");

  private ShellTable table = new ShellTable();

  private SelectorPreview selectorPreview;

  private void init() {
    selectorPreview = new SelectorPreview(repositoryManager, contentType, selector);
    selectorPreview.postExecute = () -> table.print(System.out);
    selectorPreview.preRepository = () -> {
      table.column(SELECTOR);
      table.column(REPOSITORY_NAME);
    };
    selectorPreview.eachRepository = r -> table.addRow().addContent(selector, r.getName());
    selectorPreview.preComponent = () -> {
      table.column(SELECTOR);
      table.column(REPOSITORY_NAME);
      table.column(COMPONENT_NAME);
    };
    selectorPreview.eachComponent = r -> c -> table.addRow().addContent(selector, r.getName(), c.name());
    selectorPreview.preAsset = () -> {
      table.column(SELECTOR);
      table.column(REPOSITORY_NAME);
      table.column(COMPONENT_NAME);
      table.column(ASSET_NAME);
    };
    selectorPreview.eachAsset = e -> a -> table.addRow().addContent(selector, e.getKey().getName(), e.getValue().name(), a.name());
  }

  @Override
  public Object execute() throws Exception {
    init();
    selectorPreview.executePreview();
    return null;
  }
}
