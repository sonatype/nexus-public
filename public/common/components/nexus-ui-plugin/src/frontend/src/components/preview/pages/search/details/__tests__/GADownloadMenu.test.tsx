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
import { Theme } from '@radix-ui/themes';
import { fireEvent, render, screen } from '@testing-library/react';

import { GADownloadMenu } from '../GADownloadMenu';
import * as hook from '../useVersionAssets';

const asset = (over = {}) => ({
  id: 'a1',
  repository: 'maven-central',
  path: 'x/commons-lang3-3.14.0.jar',
  downloadUrl: 'http://nx/commons-lang3-3.14.0.jar',
  format: 'jar',
  extension: 'jar',
  size: 1,
  contentType: 'application/java-archive',
  lastModified: '',
  checksums: {},
  ...over,
});

const wrap = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('GADownloadMenu', () => {
  afterEach(() => jest.restoreAllMocks());

  it('disables the button with "No downloadable assets" when there are none', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [], loading: false, error: null });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    expect(screen.getByRole('button', { name: /no downloadable assets/i })).toBeDisabled();
  });

  it('shows a disabled, loading button while assets load', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [], loading: true, error: null });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    expect(screen.getByRole('button', { name: /download/i })).toBeDisabled();
  });

  it('shows a disabled "Download unavailable" button on error', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [], loading: false, error: 'boom' });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    expect(screen.getByRole('button', { name: /download unavailable/i })).toBeDisabled();
  });

  it('renders the Download control as a real anchor to the asset downloadUrl (all interaction modes)', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [asset()], loading: false, error: null });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    // A real <a href> (not an onClick handler) so right-click "Open in new tab", middle-click, and
    // keyboard activation all work. href is the API downloadUrl; rel guards against tabnabbing.
    const link = screen.getByRole('link', { name: /download/i });
    expect(link).toHaveAttribute('href', 'http://nx/commons-lang3-3.14.0.jar');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(link).toHaveAttribute('download');
    // Inline accent text color so the label/icon keep the surface button's accent color even where
    // Nexus One's anchor reset (color: inherit) would otherwise turn them black (e.g. cloud edition).
    expect(link).toHaveStyle({ color: 'var(--accent-a11)' });
  });

  it('copies the primary asset absolute download URL when Copy path is clicked', async () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [asset()], loading: false, error: null });
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    fireEvent.click(screen.getByRole('button', { name: /copy path/i }));
    // downloadUrl is already absolute (full repo URL) — copied verbatim.
    expect(writeText).toHaveBeenCalledWith('http://nx/commons-lang3-3.14.0.jar');
  });

  it('resolves a relative download URL to an absolute one before copying', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({
      assets: [asset({ downloadUrl: '/repository/maven-central/x/commons-lang3-3.14.0.jar' })],
      loading: false,
      error: null,
    });
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    fireEvent.click(screen.getByRole('button', { name: /copy path/i }));
    expect(writeText).toHaveBeenCalledWith(
      `${window.location.origin}/repository/maven-central/x/commons-lang3-3.14.0.jar`
    );
  });

  it('points the Download link at the primary artifact, skipping checksum/pom sidecars', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({
      assets: [
        asset({ id: 'sha', path: 'x/commons-lang3-3.14.0.jar.sha1', downloadUrl: 'http://nx/sha1' }),
        asset({ id: 'pom', path: 'x/commons-lang3-3.14.0.pom', downloadUrl: 'http://nx/pom' }),
        asset({ id: 'jar', path: 'x/commons-lang3-3.14.0.jar', downloadUrl: 'http://nx/jar' }),
      ],
      loading: false,
      error: null,
    });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    expect(screen.getByRole('link', { name: /download/i })).toHaveAttribute('href', 'http://nx/jar');
  });

  it('prefers the main jar over -sources/-javadoc classifier jars regardless of order', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({
      assets: [
        asset({ id: 'src', path: 'x/commons-lang3-3.14.0-sources.jar', downloadUrl: 'http://nx/sources' }),
        asset({ id: 'doc', path: 'x/commons-lang3-3.14.0-javadoc.jar', downloadUrl: 'http://nx/javadoc' }),
        asset({ id: 'main', path: 'x/commons-lang3-3.14.0.jar', downloadUrl: 'http://nx/main' }),
      ],
      loading: false,
      error: null,
    });
    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    expect(screen.getByRole('link', { name: /download/i })).toHaveAttribute('href', 'http://nx/main');
  });

  it('links to the tarball for an npm component (no .pom, no classifier)', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({
      assets: [
        asset({ id: 'sha', path: 'lodash/-/lodash-4.17.21.tgz.sha1', downloadUrl: 'http://nx/tgz.sha1' }),
        asset({ id: 'tgz', path: 'lodash/-/lodash-4.17.21.tgz', downloadUrl: 'http://nx/tgz' }),
      ],
      loading: false,
      error: null,
    });
    wrap(<GADownloadMenu gaId="npm:lodash" version="4.17.21" repository="npm-proxy" />);
    expect(screen.getByRole('link', { name: /download/i })).toHaveAttribute('href', 'http://nx/tgz');
  });

  it('falls back to the first asset when every asset is a checksum/signature sidecar', () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({
      assets: [
        asset({ id: 's1', path: 'x/lib-1.0.jar.sha1', downloadUrl: 'http://nx/first' }),
        asset({ id: 's2', path: 'x/lib-1.0.jar.md5', downloadUrl: 'http://nx/second' }),
      ],
      loading: false,
      error: null,
    });
    wrap(<GADownloadMenu gaId="maven:g:lib" version="1.0" repository="maven-central" />);
    expect(screen.getByRole('link', { name: /download/i })).toHaveAttribute('href', 'http://nx/first');
  });

  it('does not raise an unhandled rejection when the clipboard write fails', async () => {
    jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [asset()], loading: false, error: null });
    const rejections: unknown[] = [];
    const onRejection = (reason: unknown) => rejections.push(reason);
    process.on('unhandledRejection', onRejection);
    Object.assign(navigator, { clipboard: { writeText: jest.fn().mockRejectedValue(new Error('blocked')) } });

    wrap(<GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />);
    fireEvent.click(screen.getByRole('button', { name: /copy path/i }));
    await new Promise((resolve) => setTimeout(resolve, 0));
    process.off('unhandledRejection', onRejection);

    expect(rejections).toEqual([]);
    // The optimistic "Copied" affordance still appears — the copy is best-effort.
    expect(screen.getByRole('button', { name: /path copied/i })).toBeInTheDocument();
  });

  it('clears the pending copied-reset timer when unmounted', () => {
    jest.useFakeTimers();
    const setSpy = jest.spyOn(global, 'setTimeout');
    const clearSpy = jest.spyOn(global, 'clearTimeout');
    try {
      jest.spyOn(hook, 'useVersionAssets').mockReturnValue({ assets: [asset()], loading: false, error: null });
      Object.assign(navigator, { clipboard: { writeText: jest.fn().mockResolvedValue(undefined) } });
      const { unmount } = wrap(
        <GADownloadMenu gaId="maven:g:commons-lang3" version="3.14.0" repository="maven-central" />,
      );
      fireEvent.click(screen.getByRole('button', { name: /copy path/i }));

      const timerIdx = setSpy.mock.calls.findIndex((call) => call[1] === 1500);
      expect(timerIdx).toBeGreaterThanOrEqual(0);
      const copyTimer = setSpy.mock.results[timerIdx].value;

      unmount();
      expect(clearSpy).toHaveBeenCalledWith(copyTimer);
    } finally {
      setSpy.mockRestore();
      clearSpy.mockRestore();
      jest.useRealTimers();
    }
  });
});
