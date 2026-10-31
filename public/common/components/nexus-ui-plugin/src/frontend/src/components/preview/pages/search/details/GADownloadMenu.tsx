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

import React, { useEffect, useRef, useState } from 'react';
import { Button, Flex, Spinner } from '@radix-ui/themes';
import { Check, Copy, Download } from 'lucide-react';

import type { GAAsset } from '../core';
import { useVersionAssets } from './useVersionAssets';

interface GADownloadMenuProps {
  gaId: string;
  version: string | null;
  repository: string | null;
}

/** Checksum/signature sidecars that ride alongside the real artifact but are never "the download". */
const SIDECAR = /\.(sha1|sha256|sha512|md5|asc)$/i;

/** Maven secondary artifacts distinguished only by a classifier — never the primary download. */
const CLASSIFIER = /-(sources|javadoc|tests?)\.[^./]+$/i;

/**
 * The single asset a one-click download should fetch for the selected version+repository. A
 * version can carry several assets (e.g. Maven's jar + pom + checksums + -sources/-javadoc jars);
 * this is a best-effort heuristic, not a format-aware selector: drop checksum/signature sidecars,
 * then prefer the main binary — skipping the .pom and classifier jars — falling back first to any
 * non-.pom artifact and finally to the first asset so there is always something to download.
 * npm (.tgz) and PyPI (.whl/.tar.gz) artifacts are neither .pom nor classifier, so they win the
 * first pass unchanged.
 */
function primaryAsset(assets: readonly GAAsset[]): GAAsset {
  const artifacts = assets.filter((a) => !SIDECAR.test(a.path));
  const pool = artifacts.length > 0 ? artifacts : assets;
  const isPom = (a: GAAsset) => /\.pom$/i.test(a.path);
  return (
    pool.find((a) => !isPom(a) && !CLASSIFIER.test(a.path)) ??
    pool.find((a) => !isPom(a)) ??
    pool[0]
  );
}

/**
 * Header Download control. A single button that directly downloads the selected
 * version+repository's component via the API-provided downloadUrl — no dropdown, and no
 * UI-assembled URLs (security requirement).
 */
export function GADownloadMenu({ gaId, version, repository }: GADownloadMenuProps) {
  const { assets, loading, error } = useVersionAssets({ gaId, version, repository });
  const [copied, setCopied] = useState(false);
  const copiedTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clear any pending copied-reset timer on unmount so it can't fire on an unmounted component.
  useEffect(
    () => () => {
      if (copiedTimerRef.current) {
        clearTimeout(copiedTimerRef.current);
      }
    },
    [],
  );

  if (loading) {
    return (
      <Button size="2" variant="surface" disabled>
        <Spinner size="1" />
        Download
      </Button>
    );
  }

  if (error) {
    return (
      <Button size="2" variant="surface" color="red" disabled aria-label="Download unavailable">
        <Download size={14} />
        Download unavailable
      </Button>
    );
  }

  if (assets.length === 0) {
    return (
      <Button size="2" variant="surface" disabled aria-label="No downloadable assets">
        <Download size={14} />
        No downloadable assets
      </Button>
    );
  }

  const primary = primaryAsset(assets);
  // Absolute URL with the full repository host, so a copied link works anywhere — not just from a
  // browser already sitting on this origin. new URL() is a no-op when the API already returned an
  // absolute downloadUrl (the usual case) and only fills in the origin for a relative one; either
  // way the URL still comes from the API, never assembled from parts in the UI (security rule).
  // Base is window.location.href, not window.location.origin: origin serializes to the string
  // "null" for file:// or sandboxed-iframe contexts, which would produce a "null/repository/…"
  // link for a relative downloadUrl. href always carries a usable base.
  const absoluteUrl = new URL(primary.downloadUrl, window.location.href).href;

  const handleCopy = () => {
    // writeText can reject (permissions, insecure context) or be absent; swallow it so it isn't an
    // unhandled rejection — the copy is best-effort and the button still gives optimistic feedback.
    navigator.clipboard?.writeText(absoluteUrl).catch(() => {});
    setCopied(true);
    if (copiedTimerRef.current) {
      clearTimeout(copiedTimerRef.current);
    }
    copiedTimerRef.current = setTimeout(() => setCopied(false), 1500);
  };

  return (
    <Flex align="center" gap="2">
      {/* A real anchor (rendered as a Button via asChild), not an onClick={window.open}: this makes
          right-click "Open in new tab", middle-click, and keyboard activation all work, which a
          div/button with a JS click handler silently swallows. href is the API-provided downloadUrl
          (never a UI-assembled URL); target/rel preserve the prior new-tab, no-tabnabbing behavior.

          color is set INLINE to var(--accent-a11) — the surface Button's own accent text color.
          Nexus One resets bare anchors with a high-specificity App.scss rule
          (`.nxrm-main-content > .radix-themes:not(:has(.nxrm-ext-js-wrapper)) a:not([class*="x-"])`
          → color: inherit), which cascades to the label and the currentColor icon and outranks
          Radix's button color — leaving the text/icon black in editions where that selector matches
          (e.g. cloud). Only an inline color beats it. Same override technique used by GAVersionsTab. */}
      <Button size="2" variant="surface" asChild>
        <a
          href={primary.downloadUrl}
          aria-label="Download"
          download
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: 'var(--accent-a11)' }}
        >
          <Download size={14} />
          Download
        </a>
      </Button>
      <Button
        size="2"
        variant="surface"
        color="gray"
        aria-label={copied ? 'Path copied' : 'Copy path'}
        onClick={handleCopy}
      >
        {copied ? <Check size={14} /> : <Copy size={14} />}
        {copied ? 'Copied' : 'Copy path'}
      </Button>
    </Flex>
  );
}

export default GADownloadMenu;
