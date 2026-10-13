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

/**
 * Regression tests for browse-tree pointer, keyboard and ARIA behaviour:
 *
 * - single click selects, double click expands/collapses
 * - ArrowRight expands and never collapses; ArrowLeft collapses then moves out
 * - Up/Down walk *visible* nodes in preorder, so nested nodes stay reachable
 * - aria-selected tracks selection, not focus
 *
 * Uses a purpose-built child map (root branches immediately) so no auto-expansion
 * runs and each interaction is asserted against a known collapsed baseline.
 */

import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';

import { BrowseTree } from '../BrowseTree';
import type { BrowseNode, BrowseTreeRef } from '../browse-tree.types';

jest.mock('../../../../../../interface/ExtAPIUtils', () => ({
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn((response: any) => response.data.result.data),
  },
}));

jest.mock('../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    EXT: { BROWSE: { ACTION: 'coreui_Browse', METHODS: { READ: 'read' } } },
  },
}));

import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';

const mockRequest = ExtAPIUtils.extAPIRequest as jest.Mock;

function folder(id: string): BrowseNode {
  return {
    id,
    text: id.split('/').pop() as string,
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  };
}

function asset(id: string): BrowseNode {
  return {
    id,
    text: id.split('/').pop() as string,
    type: 'asset',
    leaf: true,
    componentId: null,
    assetId: `a-${id}`,
    packageUrl: null,
  };
}

/**
 *   top          (folder, 2 children -> no auto-expansion below it either)
 *     deep       (folder)
 *       inner.jar
 *     file.jar
 *   other        (folder)
 *     o.jar
 */
const MAP: Record<string, BrowseNode[]> = {
  '/': [folder('top'), folder('other')],
  top: [folder('top/deep'), asset('top/file.jar')],
  'top/deep': [asset('top/deep/inner.jar')],
  other: [asset('other/o.jar')],
};

beforeEach(() => {
  jest.clearAllMocks();
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => ({
    data: { result: { success: true, data: MAP[data[0].node] ?? [] } },
  }));
});

/** The row element: it owns the roving tabIndex and the key handler. */
function row(nodeId: string): HTMLElement {
  return screen.getByTestId(`tree-node-${nodeId}`);
}

/** The treeitem element: it owns the ARIA state. */
function item(nodeId: string): HTMLElement {
  const el = row(nodeId).closest('[role="treeitem"]');
  if (!el) throw new Error(`no treeitem for ${nodeId}`);
  return el as HTMLElement;
}

async function renderTree() {
  const utils = render(<BrowseTree repositoryName="maven-hosted" />);
  await waitFor(() => expect(screen.getByTestId('tree-node-top')).toBeInTheDocument());
  return utils;
}

describe('pointer behaviour', () => {
  it('single click selects a folder without expanding it', async () => {
    await renderTree();

    fireEvent.click(screen.getByTestId('link-top'));

    await waitFor(() => expect(item('top')).toHaveClass('browse-tree__node-container'));
    expect(item('top')).toHaveAttribute('aria-selected', 'true');
    expect(item('top')).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument();
  });

  it('double click expands a collapsed folder', async () => {
    await renderTree();

    fireEvent.doubleClick(row('top'));

    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());
    expect(item('top')).toHaveAttribute('aria-expanded', 'true');
  });

  it('double click collapses an expanded folder', async () => {
    await renderTree();

    fireEvent.doubleClick(row('top'));
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    fireEvent.doubleClick(row('top'));
    await waitFor(() =>
      expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument()
    );
    expect(item('top')).toHaveAttribute('aria-expanded', 'false');
  });

  it('double click on a leaf asset does not make it expandable', async () => {
    await renderTree();

    fireEvent.doubleClick(row('top'));
    await waitFor(() => expect(screen.getByTestId('tree-node-top/file.jar')).toBeInTheDocument());

    fireEvent.doubleClick(row('top/file.jar'));

    expect(item('top/file.jar')).not.toHaveAttribute('aria-expanded');
    expect(mockRequest).not.toHaveBeenCalledWith(
      expect.anything(),
      expect.anything(),
      expect.objectContaining({ data: [expect.objectContaining({ node: 'top/file.jar' })] })
    );
  });

  it('chevron click expands without selecting the node', async () => {
    await renderTree();

    fireEvent.click(screen.getByTestId('toggle-top'));

    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());
    expect(item('top')).toHaveAttribute('aria-selected', 'false');
  });

  it('double clicking the chevron toggles once per click and no more', async () => {
    await renderTree();
    const toggle = screen.getByTestId('toggle-top');

    // A real double click on the chevron delivers both clicks *and* a dblclick.
    // The chevron already handled the clicks, so the row-level double-click
    // handler must stay out of it: three toggles would flicker the folder open
    // and refetch its children.
    fireEvent.click(toggle);
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());
    fireEvent.click(toggle);
    fireEvent.doubleClick(toggle);

    await waitFor(() =>
      expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument()
    );
    expect(item('top')).toHaveAttribute('aria-expanded', 'false');
  });
});

describe('keyboard navigation', () => {
  it('ArrowRight expands a collapsed folder', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });

    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());
  });

  it('ArrowRight never collapses an already expanded folder', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    // Second press moves into the branch; it must not close it.
    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });

    await waitFor(() => expect(row('top/deep')).toHaveAttribute('tabindex', '0'));
    expect(item('top')).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument();
  });

  it('ArrowLeft collapses an expanded folder', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    fireEvent.keyDown(row('top'), { key: 'ArrowLeft' });

    await waitFor(() =>
      expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument()
    );
  });

  it('ArrowLeft on a collapsed node moves focus to its parent', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    fireEvent.keyDown(row('top/deep'), { key: 'ArrowLeft' });

    await waitFor(() => expect(row('top')).toHaveAttribute('tabindex', '0'));
    expect(item('top')).toHaveAttribute('aria-expanded', 'true');
  });

  it('ArrowDown walks visible nodes in preorder, descending into expanded branches', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    // top -> top/deep (a nested node, not the next root sibling)
    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });
    await waitFor(() => expect(row('top/deep')).toHaveAttribute('tabindex', '0'));
    expect(row('other')).toHaveAttribute('tabindex', '-1');

    // top/deep -> top/file.jar
    fireEvent.keyDown(row('top/deep'), { key: 'ArrowDown' });
    await waitFor(() => expect(row('top/file.jar')).toHaveAttribute('tabindex', '0'));

    // last visible descendant -> next ancestor sibling
    fireEvent.keyDown(row('top/file.jar'), { key: 'ArrowDown' });
    await waitFor(() => expect(row('other')).toHaveAttribute('tabindex', '0'));
  });

  it('nested nodes hold the roving tab stop and receive DOM focus', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });

    await waitFor(() => expect(row('top/deep')).toHaveAttribute('tabindex', '0'));
    expect(document.activeElement).toBe(row('top/deep'));
  });

  it('ArrowUp from the first node stays on the first node', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });
    await waitFor(() => expect(row('other')).toHaveAttribute('tabindex', '0'));

    fireEvent.keyDown(row('other'), { key: 'ArrowUp' });
    await waitFor(() => expect(row('top')).toHaveAttribute('tabindex', '0'));

    fireEvent.keyDown(row('top'), { key: 'ArrowUp' });
    expect(row('top')).toHaveAttribute('tabindex', '0');
  });
});

describe('tree ARIA contract', () => {
  it('aria-selected tracks selection, not focus', async () => {
    await renderTree();

    fireEvent.click(screen.getByTestId('link-top'));
    await waitFor(() => expect(item('top')).toHaveAttribute('aria-selected', 'true'));

    // Move focus only — the selection (and the detail panel) must not follow.
    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });

    await waitFor(() => expect(row('other')).toHaveAttribute('tabindex', '0'));
    expect(item('other')).toHaveAttribute('aria-selected', 'false');
    expect(item('top')).toHaveAttribute('aria-selected', 'true');
  });

  it('exposes aria-level, aria-setsize and aria-posinset for every level', async () => {
    await renderTree();

    expect(item('top')).toHaveAttribute('aria-level', '1');
    expect(item('top')).toHaveAttribute('aria-setsize', '2');
    expect(item('top')).toHaveAttribute('aria-posinset', '1');
    expect(item('other')).toHaveAttribute('aria-posinset', '2');

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    expect(item('top/deep')).toHaveAttribute('aria-level', '2');
    expect(item('top/deep')).toHaveAttribute('aria-setsize', '2');
    expect(item('top/deep')).toHaveAttribute('aria-posinset', '1');
    expect(item('top/file.jar')).toHaveAttribute('aria-posinset', '2');
  });

  it('hides the decorative chevron from assistive technology', async () => {
    await renderTree();

    // aria-expanded on the treeitem already conveys state; the chevron is a
    // pointer affordance only and must not appear as a separate control.
    expect(screen.getByTestId('toggle-top')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByTestId('toggle-top')).not.toHaveAttribute('aria-label');
  });
});

describe('focus continuity across pointer, rerenders and removal', () => {
  it('continues keyboard navigation from the node the mouse selected', async () => {
    await renderTree();

    // Open `top` so the visible order is: top, top/deep, top/file.jar, other.
    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/file.jar')).toBeInTheDocument());

    // Select the *last* child with the mouse. If ArrowDown fell back to the
    // default origin (the first node) it would land on `top/deep` instead.
    fireEvent.click(screen.getByTestId('link-top/file.jar'));
    await waitFor(() =>
      expect(item('top/file.jar')).toHaveAttribute('aria-selected', 'true')
    );

    fireEvent.keyDown(row('top/file.jar'), { key: 'ArrowDown' });

    await waitFor(() => expect(document.activeElement).toBe(row('other')));
  });

  it('keeps the focused row focused across an unrelated parent rerender', async () => {
    // Stands in for the detail panel finishing its load and rerendering Browse:
    // the tree gets new props but the user's place must not move.
    const { rerender } = render(<BrowseTree repositoryName="maven-hosted" />);
    await waitFor(() => expect(screen.getByTestId('tree-node-top')).toBeInTheDocument());

    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });
    await waitFor(() => expect(document.activeElement).toBe(row('other')));

    rerender(<BrowseTree repositoryName="maven-hosted" className="detail-loaded" />);

    expect(document.activeElement).toBe(row('other'));
  });

  it('does not pull focus back out of the detail panel once the user moves it there', async () => {
    const { rerender } = render(
      <>
        <BrowseTree repositoryName="maven-hosted" />
        <button type="button" data-testid="detail-control">
          Delete
        </button>
      </>
    );
    await waitFor(() => expect(screen.getByTestId('tree-node-top')).toBeInTheDocument());

    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });
    await waitFor(() => expect(document.activeElement).toBe(row('other')));

    // Deliberate move into the detail panel, then a rerender of the tree.
    const detailControl = screen.getByTestId('detail-control');
    act(() => detailControl.focus());
    rerender(
      <>
        <BrowseTree repositoryName="maven-hosted" className="rerendered" />
        <button type="button" data-testid="detail-control">
          Delete
        </button>
      </>
    );

    expect(document.activeElement).toBe(detailControl);
  });

  it('moves focus to a surviving neighbour when the focused node is removed', async () => {
    // Parent reload after the delete must report the node as gone, otherwise the
    // server response would simply re-add it.
    const afterDelete: Record<string, BrowseNode[]> = {
      ...MAP,
      top: [asset('top/file.jar')],
    };
    let deleted = false;
    mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => {
      const map = deleted ? afterDelete : MAP;
      return { data: { result: { success: true, data: map[data[0].node] ?? [] } } };
    });

    const ref = React.createRef<BrowseTreeRef>();
    render(<BrowseTree ref={ref} repositoryName="maven-hosted" />);
    await waitFor(() => expect(screen.getByTestId('tree-node-top')).toBeInTheDocument());

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    // Focus the node that is about to disappear.
    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });
    await waitFor(() => expect(document.activeElement).toBe(row('top/deep')));

    deleted = true;
    await act(async () => {
      await ref.current!.removeNode('top/deep');
    });

    // Focus lands on the next surviving visible row, not on <body>.
    expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument();
    await waitFor(() => expect(document.activeElement).toBe(row('top/file.jar')));
    expect(document.activeElement).not.toBe(document.body);
  });

  it('ignores keys that originate in a nested row control', async () => {
    await renderTree();

    // The tree renders no per-row action menu today, so inject a control that
    // matches the handler's nested-control selector. This asserts the guard
    // contract directly: a menu's own arrow/Enter handling must not also drive
    // tree expansion or selection.
    const menuItem = document.createElement('button');
    menuItem.setAttribute('data-testid', 'row-menu-item');
    row('top').appendChild(menuItem);

    fireEvent.keyDown(menuItem, { key: 'ArrowRight' });
    fireEvent.keyDown(menuItem, { key: 'Enter' });
    fireEvent.keyDown(menuItem, { key: 'ArrowDown' });

    expect(item('top')).toHaveAttribute('aria-expanded', 'false');
    expect(item('top')).toHaveAttribute('aria-selected', 'false');
    expect(screen.queryByTestId('tree-node-top/deep')).not.toBeInTheDocument();
  });
});

describe('collapsing an ancestor keeps the tree reachable by keyboard', () => {
  /**
   * The roving tab stop is a single tabbable row. Collapsing an ancestor unmounts
   * the focused descendant, so unless the tab stop moves with it the tree ends up
   * with *no* tabbable row: Tab skips the whole tree and only a mouse can recover
   * it. The tab stop must land on the ancestor the user just collapsed.
   */
  async function expandToLeafAndFocusIt() {
    await renderTree();

    fireEvent.doubleClick(row('top'));
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());
    fireEvent.doubleClick(row('top/deep'));
    await waitFor(() =>
      expect(screen.getByTestId('tree-node-top/deep/inner.jar')).toBeInTheDocument()
    );

    fireEvent.click(screen.getByTestId('link-top/deep/inner.jar'));
    await waitFor(() => expect(document.activeElement).toBe(row('top/deep/inner.jar')));
  }

  function tabbableRows(): HTMLElement[] {
    return Array.from(
      document.querySelectorAll<HTMLElement>('[data-testid^="tree-node-"][tabindex="0"]')
    );
  }

  it('leaves exactly one tabbable row after a double-click collapse', async () => {
    await expandToLeafAndFocusIt();

    fireEvent.doubleClick(row('top'));

    await waitFor(() =>
      expect(screen.queryByTestId('tree-node-top/deep/inner.jar')).not.toBeInTheDocument()
    );
    expect(tabbableRows()).toHaveLength(1);
    expect(tabbableRows()[0]).toBe(row('top'));
  });

  it('moves DOM focus to the collapsed ancestor rather than dropping it to the body', async () => {
    await expandToLeafAndFocusIt();

    fireEvent.doubleClick(row('top'));

    await waitFor(() => expect(document.activeElement).toBe(row('top')));
    expect(document.activeElement).not.toBe(document.body);
  });

  it('keeps the tab stop where it is when the collapse does not hide it', async () => {
    await expandToLeafAndFocusIt();

    // Collapsing an unrelated branch must not steal the tab stop.
    fireEvent.doubleClick(row('other'));

    expect(tabbableRows()).toHaveLength(1);
    expect(tabbableRows()[0]).toBe(row('top/deep/inner.jar'));
  });

  it('still traverses with the keyboard after the collapse', async () => {
    await expandToLeafAndFocusIt();

    fireEvent.doubleClick(row('top'));
    await waitFor(() => expect(document.activeElement).toBe(row('top')));

    fireEvent.keyDown(row('top'), { key: 'ArrowDown' });

    await waitFor(() => expect(document.activeElement).toBe(row('other')));
  });
});

describe('the focusable element is the treeitem', () => {
  it('puts role, ARIA state, the roving tabIndex and DOM focus on one element', async () => {
    await renderTree();

    // Same node, reached two ways: whatever holds the tab stop is what assistive
    // technology reports as the treeitem.
    expect(item('top')).toBe(row('top'));

    const tabStop = row('top');
    expect(tabStop).toHaveAttribute('role', 'treeitem');
    expect(tabStop).toHaveAttribute('aria-expanded', 'false');
    expect(tabStop).toHaveAttribute('aria-selected', 'false');
    expect(tabStop).toHaveAttribute('tabindex', '0');
    expect(row('other')).toHaveAttribute('tabindex', '-1');

    fireEvent.keyDown(tabStop, { key: 'ArrowDown' });

    await waitFor(() => expect(document.activeElement).toBe(row('other')));
    expect(document.activeElement).toHaveAttribute('role', 'treeitem');
    expect(document.activeElement).toHaveAttribute('tabindex', '0');
    expect(row('top')).toHaveAttribute('tabindex', '-1');
  });

  it('lets a nested treeitem handle its own keys without also driving its ancestors', async () => {
    await renderTree();

    fireEvent.keyDown(row('top'), { key: 'ArrowRight' });
    await waitFor(() => expect(screen.getByTestId('tree-node-top/deep')).toBeInTheDocument());

    // top/deep is rendered *inside* top's treeitem element, so its key events
    // bubble through top's handler. Expanding the child must not also move or
    // toggle the ancestor.
    fireEvent.keyDown(row('top/deep'), { key: 'ArrowRight' });

    await waitFor(() =>
      expect(screen.getByTestId('tree-node-top/deep/inner.jar')).toBeInTheDocument()
    );
    expect(item('top')).toHaveAttribute('aria-expanded', 'true');
    expect(item('top/deep')).toHaveAttribute('aria-expanded', 'true');
  });
});

describe('scrolling the selection into view', () => {
  const original = Element.prototype.scrollIntoView;

  beforeEach(() => {
    // Selecting a node schedules a scroll on the next animation frame. Hosts that
    // do not implement scrollIntoView (jsdom among them) must not break the tree.
    // @ts-expect-error deliberately removing the API to model such a host
    Element.prototype.scrollIntoView = undefined;
  });

  afterEach(() => {
    Element.prototype.scrollIntoView = original;
  });

  it('survives a host without scrollIntoView', async () => {
    await renderTree();

    fireEvent.click(screen.getByTestId('link-top'));
    await waitFor(() => expect(item('top')).toHaveAttribute('aria-selected', 'true'));

    // Let the animation-frame callback run while the tree is still mounted; an
    // unguarded call would surface here as an uncaught TypeError.
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 60));
    });

    expect(item('top')).toHaveAttribute('aria-selected', 'true');
  });
});
