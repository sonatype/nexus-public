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
import type { GolangDetail } from './golang.types';
import { mockGolangDetailApi } from './mockData';

import './GolangDetailPage.scss';

export interface GolangDetailPageProps {
  /** Module ID to display */
  moduleId: string;
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
 * Go module detail page.
 */
export function GolangDetailPage({ moduleId, onBack }: GolangDetailPageProps): JSX.Element {
  const [detail, setDetail] = useState<GolangDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    async function loadDetail(): Promise<void> {
      setLoading(true);
      setError(null);

      try {
        // TODO: Replace with real API call
        const data = await mockGolangDetailApi(moduleId);
        if (mounted) {
          setDetail(data);
        }
      } catch (err) {
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Failed to load module details');
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
  }, [moduleId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  if (loading) {
    return (
      <div className="golang-detail-page golang-detail-page--loading">
        <div className="golang-detail-page__spinner" />
        <p>Loading module details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="golang-detail-page golang-detail-page--error">
        <p className="golang-detail-page__error">{error}</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className="golang-detail-page golang-detail-page--error">
        <p>Module not found</p>
        <button type="button" onClick={handleBack}>
          Back to Search
        </button>
      </div>
    );
  }

  return (
    <div className="golang-detail-page">
      <header className="golang-detail-page__header">
        <button
          type="button"
          className="golang-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
        <h1 className="golang-detail-page__title">{detail.module}</h1>
        {detail.description && (
          <p className="golang-detail-page__description">{detail.description}</p>
        )}
      </header>

      <div className="golang-detail-page__content">
        {/* Metadata Section */}
        <section className="golang-detail-page__section">
          <h2 className="golang-detail-page__section-title">Module Info</h2>
          <dl className="golang-detail-page__metadata">
            {detail.license && (
              <>
                <dt>License</dt>
                <dd>{detail.license}</dd>
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
            {detail.repositoryUrl && (
              <>
                <dt>Repository</dt>
                <dd>
                  <a href={detail.repositoryUrl} target="_blank" rel="noopener noreferrer">
                    {detail.repositoryUrl}
                  </a>
                </dd>
              </>
            )}
          </dl>
        </section>

        {/* Versions Section */}
        <section className="golang-detail-page__section">
          <h2 className="golang-detail-page__section-title">
            Versions ({detail.versions.length})
          </h2>
          <table className="golang-detail-page__versions-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Published</th>
                <th>Repository</th>
              </tr>
            </thead>
            <tbody>
              {detail.versions.map((v) => (
                <tr key={v.version}>
                  <td className="golang-detail-page__version-cell">{v.version}</td>
                  <td>{formatDate(v.published)}</td>
                  <td>{v.repository}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Repositories Section */}
        <section className="golang-detail-page__section">
          <h2 className="golang-detail-page__section-title">
            Available In ({detail.repositories.length} repositories)
          </h2>
          <ul className="golang-detail-page__repos-list">
            {detail.repositories.map((repo) => (
              <li key={repo}>{repo}</li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}

export default GolangDetailPage;


