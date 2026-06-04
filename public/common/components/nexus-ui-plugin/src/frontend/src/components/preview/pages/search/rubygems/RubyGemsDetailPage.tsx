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

import React, { useEffect, useState } from 'react';
import type { RubyGemsDetail } from './rubygems.types';
import { mockRubyGemsDetailApi } from './mockData';

import './RubyGemsDetailPage.scss';

export interface RubyGemsDetailPageProps {
  /** Gem ID to display */
  gemId: string;
  /** Callback to go back to search */
  onBack?: () => void;
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
  });
}

/**
 * RubyGems detail page.
 */
export function RubyGemsDetailPage({ gemId, onBack }: RubyGemsDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<RubyGemsDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        // TODO: Replace with real API call
        const data = await mockRubyGemsDetailApi(gemId);
        if (mounted) {
          setDetail(data);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load gem details');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    }

    loadDetail();

    return () => {
      mounted = false;
    };
  }, [gemId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  if (loading) {
    return (
      <div className="rubygems-detail-page rubygems-detail-page--loading">
        <div className="rubygems-detail-page__spinner" />
        <p>Loading gem details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rubygems-detail-page rubygems-detail-page--error">
        <p className="rubygems-detail-page__error">{error}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="rubygems-detail-page rubygems-detail-page--error">
        <p>Gem not found</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="rubygems-detail-page">
      <header className="rubygems-detail-page__header">
        <button
          type="button"
          className="rubygems-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <h1 className="rubygems-detail-page__title">{detail.displayName}</h1>
        {detail.summary && (
          <p className="rubygems-detail-page__summary">{detail.summary}</p>
        )}
      </header>

      <div className="rubygems-detail-page__content">
        {/* Metadata Section */}
        <section className="rubygems-detail-page__section">
          <h2 className="rubygems-detail-page__section-title">Gem Info</h2>
          <dl className="rubygems-detail-page__metadata">
            {detail.authors && (
              <>
                <dt>Authors</dt>
                <dd>{detail.authors}</dd>
              </>
            )}
            {detail.licenses && detail.licenses.length > 0 && (
              <>
                <dt>License</dt>
                <dd>{detail.licenses.join(', ')}</dd>
              </>
            )}
            {detail.rubyVersion && (
              <>
                <dt>Ruby</dt>
                <dd>{detail.rubyVersion}</dd>
              </>
            )}
            {detail.homepage && (
              <>
                <dt>Homepage</dt>
                <dd>
                  <a href={detail.homepage} target="_blank" rel="noopener noreferrer">
                    {detail.homepage}
                  </a>
                </dd>
              </>
            )}
            {detail.sourceCodeUri && (
              <>
                <dt>Source Code</dt>
                <dd>
                  <a href={detail.sourceCodeUri} target="_blank" rel="noopener noreferrer">
                    {detail.sourceCodeUri}
                  </a>
                </dd>
              </>
            )}
            {detail.documentationUri && (
              <>
                <dt>Documentation</dt>
                <dd>
                  <a href={detail.documentationUri} target="_blank" rel="noopener noreferrer">
                    {detail.documentationUri}
                  </a>
                </dd>
              </>
            )}
          </dl>
        </section>

        {/* Versions Section */}
        <section className="rubygems-detail-page__section">
          <h2 className="rubygems-detail-page__section-title">
            Versions ({detail.versions.length})
          </h2>
          <table className="rubygems-detail-page__versions-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Platform</th>
                <th>Ruby</th>
                <th>Published</th>
                <th>Repository</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((v) => (
                <tr key={`${v.version}-${v.platform}`}>
                  <td className="rubygems-detail-page__version-cell">{v.version}</td>
                  <td>
                    <span className={`rubygems-detail-page__platform-badge rubygems-detail-page__platform-badge--${v.platform}`}>
                      {v.platform}
                    </span>
                  </td>
                  <td>{v.rubyVersion || '-'}</td>
                  <td>{formatDate(v.published)}</td>
                  <td>{v.repository}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Repositories Section */}
        <section className="rubygems-detail-page__section">
          <h2 className="rubygems-detail-page__section-title">
            Available In ({detail.repositories.length} repositories)
          </h2>
          <ul className="rubygems-detail-page__repos-list">
            {detail.repositories.map((repo) => (
              <li key={repo}>{repo}</li>
            ))}
          </ul>
        </section>

        {/* Description Section */}
        {detail.description && (
          <section className="rubygems-detail-page__section">
            <h2 className="rubygems-detail-page__section-title">Description</h2>
            <div className="rubygems-detail-page__description">
              <pre>{detail.description}</pre>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

export default RubyGemsDetailPage;


