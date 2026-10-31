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
 * Regression tests for deep trees and long node names.
 *
 * A 15-level chain of folders with very long names is built so that nested rows
 * are far wider than any realistic panel. What JSDOM *can* verify is asserted
 * here: every level renders, indentation keeps growing (it is never clamped to
 * hide overflow), the deepest node stays keyboard-reachable, labels are not
 * truncated out of the accessibility tree, and the tree scroll container is the
 * one that owns scrolling.
 *
 * Pixel geometry (scrollWidth > clientWidth, scrollLeft actually moving) cannot
 * be asserted here — JSDOM performs no layout and reports every box as 0x0. The
 * layout contract lives in BrowseTree.scss and is verified in a browser.
 */

import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

import { BrowseTree } from '../BrowseTree';
import type { BrowseNode } from '../browse-tree.types';

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

const DEPTH = 15;
/** Long enough that a nested row cannot fit a normal tree panel. */
const SEGMENT = 'org.sonatype.deeply.nested.artifact.directory.segment';

/** Path of the folder at the given 1-based level. */
function pathAt(level: number): string {
  return Array.from({ length: level }, (_, i) => `${SEGMENT}-${i + 1}`).join('/');
}

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
 * A single 15-level chain. Every level has two children (a folder and an asset)
 * so that auto-expansion of single-child chains does not kick in and each level
 * must be opened deliberately.
 */
const MAP: Record<string, BrowseNode[]> = { '/': [folder(pathAt(1)), asset('root-file.jar')] };
for (let level = 1; level < DEPTH; level++) {
  MAP[pathAt(level)] = [folder(pathAt(level + 1)), asset(`${pathAt(level)}/file.jar`)];
}
MAP[pathAt(DEPTH)] = [asset(`${pathAt(DEPTH)}/deepest.jar`)];

beforeEach(() => {
  jest.clearAllMocks();
  mockRequest.mockImplementation(async (_a: string, _m: string, { data }: any) => ({
    data: { result: { success: true, data: MAP[data[0].node] ?? [] } },
  }));
});

function row(nodeId: string): HTMLElement {
  return screen.getByTestId(`tree-node-${nodeId}`);
}

function item(nodeId: string): HTMLElement {
  const el = row(nodeId).closest('[role="treeitem"]');
  if (!el) throw new Error(`no treeitem for ${nodeId}`);
  return el as HTMLElement;
}

/** Open every level with the keyboard, exactly as a keyboard-only user would. */
async function expandAllLevelsWithKeyboard(): Promise<void> {
  for (let level = 1; level <= DEPTH; level++) {
    fireEvent.keyDown(row(pathAt(level)), { key: 'ArrowRight' });
    // eslint-disable-next-line no-await-in-loop
    await waitFor(() => expect(item(pathAt(level))).toHaveAttribute('aria-expanded', 'true'));
  }
}

async function renderTree() {
  const utils = render(<BrowseTree repositoryName="maven-hosted" />);
  await waitFor(() => expect(screen.getByTestId(`tree-node-${pathAt(1)}`)).toBeInTheDocument());
  return utils;
}

describe('deep trees and long names', () => {
  it('renders all 15 levels with a correct aria-level for each', async () => {
    await renderTree();

    await expandAllLevelsWithKeyboard();

    for (let level = 1; level <= DEPTH; level++) {
      expect(item(pathAt(level))).toHaveAttribute('aria-level', String(level));
    }
  });

  it('keeps increasing indentation with depth instead of clamping it to fit', async () => {
    await renderTree();
    await expandAllLevelsWithKeyboard();

    const indents = Array.from({ length: DEPTH }, (_, i) =>
      Number.parseInt(
        (screen.getByTestId(`tree-row-${pathAt(i + 1)}`) as HTMLElement).style.paddingLeft,
        10
      )
    );

    for (let i = 1; i < indents.length; i++) {
      expect(indents[i]).toBeGreaterThan(indents[i - 1]);
    }
  });

  it('keeps the deepest node keyboard-reachable and focusable', async () => {
    await renderTree();
    await expandAllLevelsWithKeyboard();

    const deepest = `${pathAt(DEPTH)}/deepest.jar`;
    fireEvent.keyDown(row(pathAt(DEPTH)), { key: 'ArrowDown' });

    await waitFor(() => expect(row(deepest)).toHaveAttribute('tabindex', '0'));
    expect(document.activeElement).toBe(row(deepest));
  });

  it('exposes the full long name — labels are not shortened in the DOM', async () => {
    await renderTree();
    await expandAllLevelsWithKeyboard();

    const label = screen.getByTestId(`link-${pathAt(DEPTH)}`);

    expect(label).toHaveTextContent(`${SEGMENT}-${DEPTH}`);
    expect(label.textContent).not.toContain('…');
  });

  it('scrolls a node selected by click into view, honouring the inline axis', async () => {
    const scrollIntoView = jest.fn();
    // JSDOM does not implement scrollIntoView.
    (Element.prototype as any).scrollIntoView = scrollIntoView;

    await renderTree();
    await expandAllLevelsWithKeyboard();

    fireEvent.click(screen.getByTestId(`link-${pathAt(DEPTH)}`));

    await waitFor(() => expect(scrollIntoView).toHaveBeenCalled());
    const options = scrollIntoView.mock.calls.at(-1)?.[0];
    expect(options).toMatchObject({ inline: expect.any(String) });
  });

  it('gives the tree its own scroll container rather than scrolling the page', async () => {
    await renderTree();

    // The element carrying role="tree" is the scroll owner; overflow lives on
    // .browse-tree in BrowseTree.scss.
    const tree = screen.getByRole('tree');

    expect(tree).toHaveClass('browse-tree');
  });
});
