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

import React, { useState, useEffect, useCallback } from 'react';
import type { DockerDetail, DockerDetailState, DockerTag } from './docker.types';
// TODO: replace with real API call when migrating to classic UI API (pre-existing from PR #15732, NEXUS-51698)
import { mockDetailApi } from './mockData';
import { sanitizeRegistryUrl } from '../../browse/detail/detail.utils';

import './DockerDetailPage.scss';

export interface DockerDetailPageProps {
  /** Docker image ID to display */
  id: string;
  /** Callback when navigating back to search */
  onNavigateBack?: () => void;
}

/**
 * Formats a date string for display.
 */
function formatDate(isoDate: string): string {
  const date = new Date(isoDate);
  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * Formats a byte count as MB (fallback for image-level totalSize). Matches
 * the "rounded-to-2dp MB" convention used on the metadata panel (NEXUS-51972).
 */
function formatSizeMb(bytes: number): string {
  if (bytes <= 0) return '0 MB';
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(2)} MB`;
}

/**
 * Returns true when the metadata object has at least one displayable field.
 * Used to hide the whole panel when the backend hasn't populated anything yet.
 */
function hasMetadata(
  metadata: import('./docker.types').DockerImageMetadata | undefined,
): boolean {
  if (!metadata) return false;
  const { os, arch, created, author, workingDir, totalSize, exposedPorts, cmd, entrypoint, env } =
    metadata;
  return Boolean(
    os ||
      arch ||
      created ||
      author ||
      workingDir ||
      (typeof totalSize === 'number' && totalSize > 0) ||
      (exposedPorts && exposedPorts.length > 0) ||
      (cmd && cmd.length > 0) ||
      (entrypoint && entrypoint.length > 0) ||
      (env && env.length > 0),
  );
}

/**
 * Docker image detail page component.
 *
 * Shows:
 * - Image name and description
 * - Tags table with digest, size, pushed date, os/arch
 * - Pull command
 */
export function DockerDetailPage({
  id,
  onNavigateBack,
}: DockerDetailPageProps): JSX.Element {
  const [state, setState] = useState<DockerDetailState>({
    loading: true,
    error: undefined,
    detail: undefined,
  });

  // Load detail on mount
  useEffect(() => {
    const loadDetail = async (): Promise<void> => {
      setState({ loading: true, error: undefined, detail: undefined });

      try {
        // TODO: Replace with real API call
        const detail = await mockDetailApi(id);

        if (!detail) {
          setState({
            loading: false,
            error: 'Image not found',
            detail: undefined,
          });
          return;
        }

        setState({
          loading: false,
          error: undefined,
          detail,
        });
      } catch (error) {
        setState({
          loading: false,
          error: error instanceof Error ? error.message : 'Failed to load image details',
          detail: undefined,
        });
      }
    };

    loadDetail();
  }, [id]);

  const handleBack = useCallback((): void => {
    if (onNavigateBack) {
      onNavigateBack();
    } else {
      window.location.hash = '#preview/browse/search/docker';
    }
  }, [onNavigateBack]);

  const handleCopyCommand = useCallback(
    (imageName: string, tag: string, registryUrl?: string): void => {
      const pullTarget = sanitizeRegistryUrl(registryUrl) ? `${sanitizeRegistryUrl(registryUrl)}/${imageName}` : imageName;
      const command = `docker pull ${pullTarget}:${tag}`;
      navigator.clipboard.writeText(command);
    },
    [],
  );

  // Loading state
  if (state.loading) {
    return (
      <div className="docker-detail-page docker-detail-page--loading">
        <div className="docker-detail-page__loading-spinner" />
        <p className="docker-detail-page__loading-text">Loading image details...</p>
      </div>
    );
  }

  // Error state
  if (state.error || !state.detail) {
    return (
      <div className="docker-detail-page docker-detail-page--error">
        <p className="docker-detail-page__error-text">
          {state.error ?? 'Image not found'}
        </p>
        <button
          type="button"
          className="docker-detail-page__back-button"
          onClick={handleBack}
        >
          Back to Search
        </button>
      </div>
    );
  }

  const { detail } = state;
  // Prefix the registry host onto the image name when the attribute is present
  // so pull snippets are copy-paste-runnable against the local Nexus (NEXUS-51972).
  const pullTarget = sanitizeRegistryUrl(detail.registryUrl)
    ? `${sanitizeRegistryUrl(detail.registryUrl)}/${detail.imageName}`
    : detail.imageName;

  return (
    <div className="docker-detail-page">
      <header className="docker-detail-page__header">
        <button
          type="button"
          className="docker-detail-page__back-link"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <h1 className="docker-detail-page__title">{detail.displayName}</h1>
        <p className="docker-detail-page__image-name">{detail.imageName}</p>
        {detail.description && (
          <p className="docker-detail-page__description">{detail.description}</p>
        )}
      </header>

      <section className="docker-detail-page__info">
        <div className="docker-detail-page__info-item">
          <span className="docker-detail-page__info-label">Repository:</span>
          <span className="docker-detail-page__info-value">{detail.repository}</span>
        </div>
        <div className="docker-detail-page__info-item">
          <span className="docker-detail-page__info-label">Tags:</span>
          <span className="docker-detail-page__info-value">{detail.tagsCount}</span>
        </div>
        <div className="docker-detail-page__info-item">
          <span className="docker-detail-page__info-label">Last Updated:</span>
          <span className="docker-detail-page__info-value">{formatDate(detail.lastUpdated)}</span>
        </div>
      </section>

      {hasMetadata(detail.metadata) && (
        <section className="docker-detail-page__metadata">
          <h2 className="docker-detail-page__section-title">Image Metadata</h2>
          <dl className="docker-detail-page__metadata-list">
            {detail.metadata?.os && (
              <>
                <dt>OS</dt>
                <dd>{detail.metadata.os}</dd>
              </>
            )}
            {detail.metadata?.arch && (
              <>
                <dt>Architecture</dt>
                <dd>{detail.metadata.arch}</dd>
              </>
            )}
            {detail.metadata?.created && (
              <>
                <dt>Created</dt>
                <dd>{formatDate(detail.metadata.created)}</dd>
              </>
            )}
            {detail.metadata?.author && (
              <>
                <dt>Author</dt>
                <dd>{detail.metadata.author}</dd>
              </>
            )}
            {detail.metadata?.workingDir && (
              <>
                <dt>Working Directory</dt>
                <dd>
                  <code>{detail.metadata.workingDir}</code>
                </dd>
              </>
            )}
            {typeof detail.metadata?.totalSize === 'number' && detail.metadata.totalSize > 0 && (
              <>
                <dt>Total Size</dt>
                <dd>{formatSizeMb(detail.metadata.totalSize)}</dd>
              </>
            )}
            {detail.metadata?.exposedPorts && detail.metadata.exposedPorts.length > 0 && (
              <>
                <dt>Exposed Ports</dt>
                <dd>{detail.metadata.exposedPorts.join(', ')}</dd>
              </>
            )}
            {detail.metadata?.entrypoint && detail.metadata.entrypoint.length > 0 && (
              <>
                <dt>Entrypoint</dt>
                <dd>
                  <code>{detail.metadata.entrypoint.join(' ')}</code>
                </dd>
              </>
            )}
            {detail.metadata?.cmd && detail.metadata.cmd.length > 0 && (
              <>
                <dt>Cmd</dt>
                <dd>
                  <code>{detail.metadata.cmd.join(' ')}</code>
                </dd>
              </>
            )}
            {detail.metadata?.env && detail.metadata.env.length > 0 && (
              <>
                <dt>Environment</dt>
                <dd>
                  <ul className="docker-detail-page__metadata-env">
                    {detail.metadata.env.map((entry) => (
                      <li key={entry}>
                        <code>{entry}</code>
                      </li>
                    ))}
                  </ul>
                </dd>
              </>
            )}
          </dl>
        </section>
      )}

      <section className="docker-detail-page__pull-command">
        <h2 className="docker-detail-page__section-title">Pull Command</h2>
        <div className="docker-detail-page__command">
          <code>docker pull {pullTarget}:latest</code>
          <button
            type="button"
            className="docker-detail-page__copy-button"
            onClick={() => handleCopyCommand(detail.imageName, 'latest', detail.registryUrl)}
            title="Copy to clipboard"
          >
            📋
          </button>
        </div>
      </section>

      <section className="docker-detail-page__tags">
        <h2 className="docker-detail-page__section-title">
          Tags ({detail.tags.length})
        </h2>
        <div className="docker-detail-page__tags-table-wrapper">
          <table className="docker-detail-page__tags-table">
            <thead>
              <tr>
                <th>Tag</th>
                <th>Digest</th>
                <th>Size</th>
                <th>OS/Arch</th>
                <th>Pushed</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {detail.tags.map((tag: DockerTag) => (
                <tr key={tag.name}>
                  <td>
                    <code className="docker-detail-page__tag-name">{tag.name}</code>
                  </td>
                  <td>
                    <code className="docker-detail-page__digest">
                      {tag.digest.substring(0, 19)}...
                    </code>
                  </td>
                  <td>{tag.size ?? '-'}</td>
                  <td>
                    {tag.os && tag.architecture
                      ? `${tag.os}/${tag.architecture}`
                      : '-'}
                  </td>
                  <td>{formatDate(tag.pushedAt)}</td>
                  <td>
                    <button
                      type="button"
                      className="docker-detail-page__copy-button docker-detail-page__copy-button--small"
                      onClick={() => handleCopyCommand(detail.imageName, tag.name, detail.registryUrl)}
                      title="Copy pull command"
                    >
                      📋
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

export default DockerDetailPage;


