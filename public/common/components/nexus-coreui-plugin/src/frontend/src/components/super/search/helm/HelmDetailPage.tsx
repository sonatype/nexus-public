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
import type { HelmDetail } from './helm.types';
import { mockHelmDetailApi } from './mockData';

import './HelmDetailPage.scss';

export interface HelmDetailPageProps {
  /** Chart ID to display */
  chartId: string;
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
 * Helm chart detail page.
 */
export function HelmDetailPage({ chartId, onBack }: HelmDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<HelmDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        // TODO: Replace with real API call
        const data = await mockHelmDetailApi(chartId);
        if (mounted) {
          setDetail(data);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load chart details');
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
  }, [chartId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  if (loading) {
    return (
      <div className="helm-detail-page helm-detail-page--loading">
        <div className="helm-detail-page__spinner" />
        <p>Loading chart details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="helm-detail-page helm-detail-page--error">
        <p className="helm-detail-page__error">{error}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="helm-detail-page helm-detail-page--error">
        <p>Chart not found</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="helm-detail-page">
      <header className="helm-detail-page__header">
        <button
          type="button"
          className="helm-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <div className="helm-detail-page__title-wrapper">
          {detail.icon && (
            <img 
              src={detail.icon} 
              alt="" 
              className="helm-detail-page__icon"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          )}
          <div>
            <h1 className="helm-detail-page__title">{detail.displayName}</h1>
            {detail.description && (
              <p className="helm-detail-page__description">{detail.description}</p>
            )}
          </div>
        </div>
      </header>

      <div className="helm-detail-page__content">
        {/* Metadata Section */}
        <section className="helm-detail-page__section">
          <h2 className="helm-detail-page__section-title">Chart Info</h2>
          <dl className="helm-detail-page__metadata">
            {detail.home && (
              <>
                <dt>Home</dt>
                <dd>
                  <a href={detail.home} target="_blank" rel="noopener noreferrer">
                    {detail.home}
                  </a>
                </dd>
              </>
            )}
            {detail.sources && detail.sources.length > 0 && (
              <>
                <dt>Sources</dt>
                <dd>
                  {detail.sources.map((source, index) => (
                    <span key={source}>
                      <a href={source} target="_blank" rel="noopener noreferrer">
                        {source}
                      </a>
                      {index < detail.sources!.length - 1 && ', '}
                    </span>
                  ))}
                </dd>
              </>
            )}
            {detail.maintainers && detail.maintainers.length > 0 && (
              <>
                <dt>Maintainers</dt>
                <dd className="helm-detail-page__maintainers">
                  {detail.maintainers.map((m) => (
                    <span key={m.name} className="helm-detail-page__maintainer">
                      {m.url ? (
                        <a href={m.url} target="_blank" rel="noopener noreferrer">
                          {m.name}
                        </a>
                      ) : (
                        m.name
                      )}
                      {m.email && ` <${m.email}>`}
                    </span>
                  ))}
                </dd>
              </>
            )}
            {detail.keywords && detail.keywords.length > 0 && (
              <>
                <dt>Keywords</dt>
                <dd className="helm-detail-page__keywords">
                  {detail.keywords.map((kw) => (
                    <span key={kw} className="helm-detail-page__keyword">
                      {kw}
                    </span>
                  ))}
                </dd>
              </>
            )}
          </dl>
        </section>

        {/* Versions Section */}
        <section className="helm-detail-page__section">
          <h2 className="helm-detail-page__section-title">
            Versions ({detail.versions.length})
          </h2>
          <table className="helm-detail-page__versions-table">
            <thead>
              <tr>
                <th>Chart Version</th>
                <th>App Version</th>
                <th>Created</th>
                <th>Repository</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((v) => (
                <tr key={v.version}>
                  <td className="helm-detail-page__version-cell">{v.version}</td>
                  <td>{v.appVersion || '-'}</td>
                  <td>{formatDate(v.created)}</td>
                  <td>{v.repository}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Repositories Section */}
        <section className="helm-detail-page__section">
          <h2 className="helm-detail-page__section-title">
            Available In ({detail.repositories.length} repositories)
          </h2>
          <ul className="helm-detail-page__repos-list">
            {detail.repositories.map((repo) => (
              <li key={repo}>{repo}</li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}

export default HelmDetailPage;


