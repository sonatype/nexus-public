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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { SupportZipHaModal } from '../SupportZipHaModal';
import { DEFAULT_SUPPORT_ZIP_PARAMS, NodeInfo } from '../types';

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

const node: NodeInfo = {
  nodeId: 'node-a',
  hostname: 'host-a',
  status: 'NOT_CREATED',
};

interface RenderOpts {
  open?: boolean;
  targetNode?: NodeInfo | null;
  allNodes?: boolean;
  disabled?: boolean;
}

function renderModal(opts: RenderOpts = {}) {
  const onOpenChange = jest.fn();
  const onSubmit = jest.fn();
  const onParamChange = jest.fn();

  const utils = render(
    <SupportZipHaModal
      open={opts.open ?? true}
      onOpenChange={onOpenChange}
      targetNode={opts.targetNode === undefined ? node : opts.targetNode}
      allNodes={opts.allNodes}
      params={DEFAULT_SUPPORT_ZIP_PARAMS}
      onParamChange={onParamChange}
      onSubmit={onSubmit}
      disabled={opts.disabled}
    />,
    { wrapper: TestWrapper }
  );

  return { ...utils, onOpenChange, onSubmit, onParamChange };
}

describe('SupportZipHaModal', () => {
  it('renders the modal with the single-node title containing the hostname', () => {
    renderModal();

    expect(screen.getByTestId('support-zip-ha-modal')).toBeInTheDocument();
    expect(screen.getByText('Generate Support ZIP for host-a')).toBeInTheDocument();
  });

  it('renders the all-nodes title when allNodes is true', () => {
    renderModal({ allNodes: true, targetNode: null });

    expect(screen.getByText('Generate Support ZIP for all nodes')).toBeInTheDocument();
  });

  it('falls back to nodeId in the title when no hostname is set', () => {
    renderModal({
      targetNode: { ...node, hostname: '' },
    });

    expect(screen.getByText('Generate Support ZIP for node-a')).toBeInTheDocument();
  });

  it('does not render the modal when open is false', () => {
    renderModal({ open: false });

    expect(screen.queryByTestId('support-zip-ha-modal')).not.toBeInTheDocument();
  });

  it('renders the embedded form contents (Contents/Options sections) without its own action row', () => {
    renderModal();

    expect(screen.getByText('Contents')).toBeInTheDocument();
    expect(screen.getByText('Options')).toBeInTheDocument();

    // Form's own submit button is hidden via hideActions; only the modal's buttons exist
    expect(screen.queryByTestId('support-zip-create-button')).not.toBeInTheDocument();
    expect(screen.queryByText('Create support ZIP')).not.toBeInTheDocument();
  });

  it('closes the modal when Cancel is clicked', () => {
    const { onOpenChange, onSubmit } = renderModal();

    fireEvent.click(screen.getByTestId('support-zip-ha-modal-cancel'));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit when Generate is clicked', () => {
    const { onSubmit } = renderModal();

    fireEvent.click(screen.getByTestId('support-zip-ha-modal-confirm'));

    expect(onSubmit).toHaveBeenCalled();
  });

  it('disables the Generate button while disabled is true', () => {
    renderModal({ disabled: true });

    expect(screen.getByTestId('support-zip-ha-modal-confirm')).toBeDisabled();
  });
});
