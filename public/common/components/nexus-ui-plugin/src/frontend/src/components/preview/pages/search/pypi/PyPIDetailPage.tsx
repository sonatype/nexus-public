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
import type { PyPIDetail } from './pypi.types';
import { mockPyPIDetailApi } from './mockData';

import './PyPIDetailPage.scss';

export interface PyPIDetailPageProps {
  /** Package ID to display */
  packageId: string;
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
 * PyPI package detail page.
 */
export function PyPIDetailPage({ packageId, onBack }: PyPIDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<PyPIDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        // TODO: Replace with real API call
        const data = await mockPyPIDetailApi(packageId);
        if (mounted) {
          setDetail(data);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load package details');
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
  }, [packageId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  if (loading) {
    return (
      <div className="pypi-detail-page pypi-detail-page--loading">
        <div className="pypi-detail-page__spinner" />
        <p>Loading package details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="pypi-detail-page pypi-detail-page--error">
        <p className="pypi-detail-page__error">{error}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="pypi-detail-page pypi-detail-page--error">
        <p>Package not found</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="pypi-detail-page">
      <header className="pypi-detail-page__header">
        <button
          type="button"
          className="pypi-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <h1 className="pypi-detail-page__title">{detail.displayName}</h1>
        {detail.summary && (
          <p className="pypi-detail-page__summary">{detail.summary}</p>
        )}
      </header>

      <div className="pypi-detail-page__content">
        {/* Metadata Section */}
        <section className="pypi-detail-page__section">
          <h2 className="pypi-detail-page__section-title">Package Info</h2>
          <dl className="pypi-detail-page__metadata">
            {detail.author && (
              <>
                <dt>Author</dt>
                <dd>{detail.author}</dd>
              </>
            )}
            {detail.authorEmail && (
              <>
                <dt>Email</dt>
                <dd>
                  <a href={`mailto:${detail.authorEmail}`}>{detail.authorEmail}</a>
                </dd>
              </>
            )}
            {detail.license && (
              <>
                <dt>License</dt>
                <dd>{detail.license}</dd>
              </>
            )}
            {detail.requiresPython && (
              <>
                <dt>Python</dt>
                <dd>{detail.requiresPython}</dd>
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
            {detail.projectUrl && (
              <>
                <dt>Project URL</dt>
                <dd>
                  <a href={detail.projectUrl} target="_blank" rel="noopener noreferrer">
                    {detail.projectUrl}
                  </a>
                </dd>
              </>
            )}
            {detail.keywords && detail.keywords.length > 0 && (
              <>
                <dt>Keywords</dt>
                <dd className="pypi-detail-page__keywords">
                  {detail.keywords.map((kw) => (
                    <span key={kw} className="pypi-detail-page__keyword">
                      {kw}
                    </span>
                  ))}
                </dd>
              </>
            )}
          </dl>
        </section>

        {/* Classifiers Section */}
        {detail.classifiers && detail.classifiers.length > 0 && (
          <section className="pypi-detail-page__section">
            <h2 className="pypi-detail-page__section-title">
              Classifiers ({detail.classifiers.length})
            </h2>
            <ul className="pypi-detail-page__classifiers-list">
              {detail.classifiers.map((classifier) => (
                <li key={classifier} className="pypi-detail-page__classifier">
                  {classifier}
                </li>
              ))}
            </ul>
          </section>
        )}

        {/* Versions Section */}
        <section className="pypi-detail-page__section">
          <h2 className="pypi-detail-page__section-title">
            Versions ({detail.versions.length})
          </h2>
          <table className="pypi-detail-page__versions-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Python</th>
                <th>Published</th>
                <th>Repository</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((v) => (
                <tr key={v.version}>
                  <td className="pypi-detail-page__version-cell">{v.version}</td>
                  <td>{v.requiresPython || '-'}</td>
                  <td>{formatDate(v.published)}</td>
                  <td>{v.repository}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Repositories Section */}
        <section className="pypi-detail-page__section">
          <h2 className="pypi-detail-page__section-title">
            Available In ({detail.repositories.length} repositories)
          </h2>
          <ul className="pypi-detail-page__repos-list">
            {detail.repositories.map((repo) => (
              <li key={repo}>{repo}</li>
            ))}
          </ul>
        </section>

        {/* Description Section */}
        {detail.description && (
          <section className="pypi-detail-page__section">
            <h2 className="pypi-detail-page__section-title">Description</h2>
            <div className="pypi-detail-page__description">
              <pre>{detail.description}</pre>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

export default PyPIDetailPage;


