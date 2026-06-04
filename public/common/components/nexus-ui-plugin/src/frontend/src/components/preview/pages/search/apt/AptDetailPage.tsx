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
import type { AptDetail } from './apt.types';
import { mockAptDetailApi } from './mockData';

import './AptDetailPage.scss';

export interface AptDetailPageProps {
  /** Package ID to display */
  packageId: string;
  /** Callback when navigating back */
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
 * Apt package detail page component.
 */
export function AptDetailPage({ packageId, onBack }: AptDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<AptDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  useEffect(() => {
    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(undefined);

      try {
        const data = await mockAptDetailApi(packageId);
        setDetail(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load package details');
      } finally {
        setLoading(false);
      }
    }

    loadDetail();
  }, [packageId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.location.hash = '#preview/browse/search/apt';
    }
  };

  if (loading) {
    return (
      <div className="apt-detail-page apt-detail-page--loading">
        <p>Loading package details...</p>
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="apt-detail-page apt-detail-page--error">
        <p>{error || 'Package not found'}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="apt-detail-page">
      <header className="apt-detail-page__header">
        <button
          type="button"
          className="apt-detail-page__back-btn"
          onClick={handleBack}
          aria-label="Back to search"
        >
          ← Back to Search
        </button>
        <h1 className="apt-detail-page__title">{detail.displayName}</h1>
        {detail.description && (
          <p className="apt-detail-page__description">{detail.description}</p>
        )}
      </header>

      <div className="apt-detail-page__content">
        <section className="apt-detail-page__section">
          <h2 className="apt-detail-page__section-title">Package Info</h2>
          <dl className="apt-detail-page__info-list">
            <div className="apt-detail-page__info-item">
              <dt>Name</dt>
              <dd>{detail.name}</dd>
            </div>
            {detail.maintainer && (
              <div className="apt-detail-page__info-item">
                <dt>Maintainer</dt>
                <dd>{detail.maintainer}</dd>
              </div>
            )}
            {detail.section && (
              <div className="apt-detail-page__info-item">
                <dt>Section</dt>
                <dd>{detail.section}</dd>
              </div>
            )}
            {detail.priority && (
              <div className="apt-detail-page__info-item">
                <dt>Priority</dt>
                <dd>{detail.priority}</dd>
              </div>
            )}
            {detail.homepage && (
              <div className="apt-detail-page__info-item">
                <dt>Homepage</dt>
                <dd>
                  <a href={detail.homepage} target="_blank" rel="noopener noreferrer">
                    {detail.homepage}
                  </a>
                </dd>
              </div>
            )}
            {detail.repositories.length > 0 && (
              <div className="apt-detail-page__info-item">
                <dt>Repositories</dt>
                <dd>{detail.repositories.join(', ')}</dd>
              </div>
            )}
          </dl>
        </section>

        {detail.depends && detail.depends.length > 0 && (
          <section className="apt-detail-page__section">
            <h2 className="apt-detail-page__section-title">Dependencies</h2>
            <ul className="apt-detail-page__dep-list">
              {detail.depends.map((dep, index) => (
                <li key={index} className="apt-detail-page__dep-item">
                  {dep}
                </li>
              ))}
            </ul>
          </section>
        )}

        {detail.recommends && detail.recommends.length > 0 && (
          <section className="apt-detail-page__section">
            <h2 className="apt-detail-page__section-title">Recommends</h2>
            <ul className="apt-detail-page__dep-list">
              {detail.recommends.map((rec, index) => (
                <li key={index} className="apt-detail-page__dep-item apt-detail-page__dep-item--recommends">
                  {rec}
                </li>
              ))}
            </ul>
          </section>
        )}

        <section className="apt-detail-page__section">
          <h2 className="apt-detail-page__section-title">Versions</h2>
          <table className="apt-detail-page__versions-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Architecture</th>
                <th>Distribution</th>
                <th>Component</th>
                <th>Repository</th>
                <th>Published</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((version, index) => (
                <tr key={index}>
                  <td className="apt-detail-page__version-cell">
                    {version.version}
                  </td>
                  <td>{version.architecture}</td>
                  <td>{version.distribution || '-'}</td>
                  <td>{version.component || '-'}</td>
                  <td>{version.repository}</td>
                  <td>{formatDate(version.published)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </div>
  );
}

export default AptDetailPage;


