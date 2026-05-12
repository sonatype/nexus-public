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
import type { YumDetail } from './yum.types';
import { mockYumDetailApi } from './mockData';

import './YumDetailPage.scss';

export interface YumDetailPageProps {
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
 * Yum/RPM package detail page.
 */
export function YumDetailPage({ packageId, onBack }: YumDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<YumDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        // TODO: Replace with real API call
        const data = await mockYumDetailApi(packageId);
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
      <div className="yum-detail-page yum-detail-page--loading">
        <div className="yum-detail-page__spinner" />
        <p>Loading package details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="yum-detail-page yum-detail-page--error">
        <p className="yum-detail-page__error">{error}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="yum-detail-page yum-detail-page--error">
        <p>Package not found</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="yum-detail-page">
      <header className="yum-detail-page__header">
        <button
          type="button"
          className="yum-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <h1 className="yum-detail-page__title">{detail.displayName}</h1>
        {detail.summary && (
          <p className="yum-detail-page__summary">{detail.summary}</p>
        )}
      </header>

      <div className="yum-detail-page__content">
        {/* Metadata Section */}
        <section className="yum-detail-page__section">
          <h2 className="yum-detail-page__section-title">Package Info</h2>
          <dl className="yum-detail-page__metadata">
            {detail.license && (
              <>
                <dt>License</dt>
                <dd>{detail.license}</dd>
              </>
            )}
            {detail.vendor && (
              <>
                <dt>Vendor</dt>
                <dd>{detail.vendor}</dd>
              </>
            )}
            {detail.group && (
              <>
                <dt>Group</dt>
                <dd>{detail.group}</dd>
              </>
            )}
            {detail.url && (
              <>
                <dt>URL</dt>
                <dd>
                  <a href={detail.url} target="_blank" rel="noopener noreferrer">
                    {detail.url}
                  </a>
                </dd>
              </>
            )}
          </dl>
        </section>

        {/* Versions Section */}
        <section className="yum-detail-page__section">
          <h2 className="yum-detail-page__section-title">
            Versions ({detail.versions.length})
          </h2>
          <table className="yum-detail-page__versions-table">
            <thead>
              <tr>
                <th>Version-Release</th>
                <th>Arch</th>
                <th>Published</th>
                <th>Repository</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((v) => (
                <tr key={`${v.versionRelease}-${v.architecture}`}>
                  <td className="yum-detail-page__version-cell">{v.versionRelease}</td>
                  <td>
                    <span className="yum-detail-page__arch-badge">{v.architecture}</span>
                  </td>
                  <td>{formatDate(v.published)}</td>
                  <td>{v.repository}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Repositories Section */}
        <section className="yum-detail-page__section">
          <h2 className="yum-detail-page__section-title">
            Available In ({detail.repositories.length} repositories)
          </h2>
          <ul className="yum-detail-page__repos-list">
            {detail.repositories.map((repo) => (
              <li key={repo}>{repo}</li>
            ))}
          </ul>
        </section>

        {/* Description Section */}
        {detail.description && (
          <section className="yum-detail-page__section">
            <h2 className="yum-detail-page__section-title">Description</h2>
            <div className="yum-detail-page__description">
              <pre>{detail.description}</pre>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

export default YumDetailPage;


