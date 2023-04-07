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
import React from 'react';
import axios from 'axios';
import {render, screen, waitForElementToBeRemoved, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import BrowseTree from './BrowseTree';
import {COMPONENTS, FOLDER1_CHILDREN, COMPONENT1_CHILDREN} from './BrowseTree.testdata';

jest.mock('axios', () => ({
  post: jest.fn()
}));

describe('BrowseTree', function () {
  const repoId = 'maven-releases';
  const selectors = {
    ...TestUtils.selectors,
    getAlert: () => screen.getByRole('alert'),
    getEmptyMessage: () => screen.getByText('No component/assets found in repository'),
    getIcon: (itemLabel) => within(itemLabel).getByRole('img', {hidden: true}),
    getItemLink: (itemLabel) => within(itemLabel).getByRole('link'),
    getToggleIcon: (treeItem) => treeItem.querySelector('.nx-tree__collapse-click'),
    getTree: () => screen.getByRole('tree'),
    getTreeItems: (tree) => within(tree).getAllByRole('treeitem'),
    getTreeItemLabel: (treeItem) => treeItem.querySelector('.nx-tree__item-label')
  };

  async function renderView(data = COMPONENTS) {
    axios.post.mockResolvedValue({data: TestUtils.makeExtResult(data)});
    render(<BrowseTree itemId={repoId}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  it('renders the resolved empty text', async function() {
    await renderView([]);

    expect(selectors.getEmptyMessage()).toBeInTheDocument();
  });

  it('renders the error message', async function() {
    axios.post.mockRejectedValue({message: 'Error'});
    render(<BrowseTree itemId={repoId}/>);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    const error = selectors.getAlert();

    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('Error');
  });

  it('renders the resolved data', async function() {
    await renderView();
    const tree = selectors.getTree();
    const treeItems = selectors.getTreeItems(tree);

    expect(treeItems).toHaveLength(3);
    COMPONENTS.forEach((item, index) => {
      expect(treeItems[index]).toHaveTextContent(item['text']);
    });
  });

  it('renders the correct icon based on types', async function() {
    await renderView();
    const tree = selectors.getTree();
    const treeItems = selectors.getTreeItems(tree);

    // type=folder
    const folderItemLabel = selectors.getTreeItemLabel(treeItems[0]);
    const folderItemIcon = selectors.getIcon(folderItemLabel);
    expect(folderItemIcon).toHaveAttribute('data-icon', 'folder');

    // type=component
    const componentItemLabel = selectors.getTreeItemLabel(treeItems[1]);
    const componentItemIcon = selectors.getIcon(componentItemLabel);
    expect(componentItemIcon).toHaveAttribute('data-icon', 'box');

    // type=asset
    const assetItemLabel = selectors.getTreeItemLabel(treeItems[2]);
    const assetItemIcon = selectors.getIcon(assetItemLabel);
    expect(assetItemIcon).toHaveAttribute('data-icon', 'file-archive');
  });

  it('activates the link on click', async function() {
    await renderView();
    const tree = selectors.getTree();
    const treeItems = selectors.getTreeItems(tree);

    // click one link
    const firstTreeItemLabel = selectors.getTreeItemLabel(treeItems[0]);
    const firstTreeItemLink = selectors.getItemLink(firstTreeItemLabel);

    userEvent.click(firstTreeItemLink);
    await waitFor(() => expect(document.activeElement).toBe(firstTreeItemLink));
    expect(window.location.hash).toEqual('#browse/browse:maven-releases:folder1');

    // click another link
    const secondTreeItemLabel = selectors.getTreeItemLabel(treeItems[1]);
    const secondTreeItemLink = selectors.getItemLink(secondTreeItemLabel);

    userEvent.click(secondTreeItemLink);
    await waitFor(() => expect(document.activeElement).toBe(secondTreeItemLink));
    expect(window.location.hash).toEqual('#browse/browse:maven-releases:component1');
  });

  it('toggles the tree open and closed when the icon is clicked', async function() {
    await renderView();
    const tree = selectors.getTree();
    const treeItems = selectors.getTreeItems(tree);
    const firstTreeItem = treeItems[0];
    const icon = selectors.getToggleIcon(firstTreeItem);

    expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false');

    userEvent.click(icon);
    await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'true'));

    userEvent.click(icon);
    await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false'));
  });

  it('renders the subtrees when the icon is clicked', async function() {
    await renderView();
    const tree = selectors.getTree();
    const treeItems = selectors.getTreeItems(tree);
    const firstTreeItem = treeItems[0];
    const icon = selectors.getToggleIcon(firstTreeItem);

    // render the children of firstTreeItem
    axios.post.mockResolvedValue({data: TestUtils.makeExtResult(FOLDER1_CHILDREN)});
    userEvent.click(icon);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const subTrees = selectors.getTreeItems(firstTreeItem);

    expect(subTrees).toHaveLength(2);
    FOLDER1_CHILDREN.forEach((item, index) => {
      expect(subTrees[index]).toHaveTextContent(item['text']);
    });

    const firstSubTree = subTrees[0];
    const firstSubTreeIcon = selectors.getToggleIcon(firstSubTree);

    // render the children of firstSubTree
    axios.post.mockResolvedValue({data: TestUtils.makeExtResult(COMPONENT1_CHILDREN)});
    userEvent.click(firstSubTreeIcon);
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const firstSubTreeChildren = selectors.getTreeItems(firstSubTree);

    expect(firstSubTreeChildren).toHaveLength(5);
    COMPONENT1_CHILDREN.forEach((item, index) => {
      expect(firstSubTreeChildren[index]).toHaveTextContent(item['text']);
    });
  });

  describe('keyboard interactions', function() {
    it('toggles the tree open and closed when arrow keys are pressed', async function() {
      await renderView();
      const tree = selectors.getTree();
      const treeItems = selectors.getTreeItems(tree);
      const firstTreeItem = treeItems[0];

      expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false');

      // open the subtree when right arrow is pressed
      userEvent.type(firstTreeItem, '{arrowright}');
      await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'true'));

      // closse the subtree when left arrow is pressed
      userEvent.type(firstTreeItem, '{arrowleft}');
      await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false'));
    });

    it('toggles the tree open and closed when arrow keys are pressed', async function() {
      await renderView();
      const tree = selectors.getTree();
      const treeItems = selectors.getTreeItems(tree);
      const firstTreeItem = treeItems[0];

      expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false');

      // open the subtree when right arrow is pressed
      userEvent.type(firstTreeItem, '{arrowright}');
      await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'true'));

      // closse the subtree when left arrow is pressed
      userEvent.type(firstTreeItem, '{arrowleft}');
      await waitFor(() => expect(firstTreeItem).toHaveAttribute('aria-expanded', 'false'));
    });

    it('activates the link when enter key is pressed', async function() {
      await renderView();
      const tree = selectors.getTree();
      const treeItems = selectors.getTreeItems(tree);

      // press enter on one link
      const firstTreeItemLabel = selectors.getTreeItemLabel(treeItems[0]);
      const firstTreeItemLink = selectors.getItemLink(firstTreeItemLabel);

      userEvent.type(firstTreeItemLink, '{enter}');
      await waitFor(() => expect(document.activeElement).toBe(firstTreeItemLink));
      expect(window.location.hash).toEqual('#browse/browse:maven-releases:folder1');

      // press enter on another link
      const secondTreeItemLabel = selectors.getTreeItemLabel(treeItems[1]);
      const secondTreeItemLink = selectors.getItemLink(secondTreeItemLabel);

      userEvent.type(secondTreeItemLink, '{enter}');
      await waitFor(() => expect(document.activeElement).toBe(secondTreeItemLink));
      expect(window.location.hash).toEqual('#browse/browse:maven-releases:component1');
    });

    it('renders the subtrees when arrow keys are pressed', async function() {
      await renderView();
      const tree = selectors.getTree();
      const treeItems = selectors.getTreeItems(tree);
      const firstTreeItem = treeItems[0];

      // render the children of firstTreeItem
      axios.post.mockResolvedValue({data: TestUtils.makeExtResult(FOLDER1_CHILDREN)});
      userEvent.type(firstTreeItem, '{arrowright}');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      const subTrees = selectors.getTreeItems(firstTreeItem);

      expect(subTrees).toHaveLength(2);
      FOLDER1_CHILDREN.forEach((item, index) => {
        expect(subTrees[index]).toHaveTextContent(item['text']);
      });

      const firstSubTree = subTrees[0];

      // render the children of firstSubTree
      axios.post.mockResolvedValue({data: TestUtils.makeExtResult(COMPONENT1_CHILDREN)});
      userEvent.type(firstSubTree, '{arrowright}');
      await waitForElementToBeRemoved(selectors.queryLoadingMask());

      const firstSubTreeChildren = selectors.getTreeItems(firstSubTree);

      expect(firstSubTreeChildren).toHaveLength(5);
      COMPONENT1_CHILDREN.forEach((item, index) => {
        expect(firstSubTreeChildren[index]).toHaveTextContent(item['text']);
      });
    });
  });
});
