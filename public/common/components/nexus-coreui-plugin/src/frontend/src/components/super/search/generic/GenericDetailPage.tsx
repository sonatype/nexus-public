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
import type { GenericResult, GenericAsset } from './generic.types';
import { FORMAT_CONFIG } from './generic.types';
import { mockGenericResults } from './mockData';

import './GenericDetailPage.scss';

export interface GenericDetailPageProps {
  /** Component ID from URL */
  componentId: string;
  /** Callback to navigate back to search */
  onBack?: () => void;
}

/**
 * Detail page for a generic component.
 * Shows component metadata and list of assets.
 */
export function GenericDetailPage({
  componentId,
  onBack,
}: GenericDetailPageProps): JSX.Element {
  const [component, setComponent] = useState<GenericResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    // TODO: Replace with real API call
    // For now, find in mock data
    setLoading(true);
    setError(undefined);

    // Simulate async fetch
    setTimeout(() => {
      const found = mockGenericResults.find((r) => r.id === componentId);
      if (found) {
        setComponent(found);
      } else {
        setError(`Component not found: ${componentId}`);
      }
      setLoading(false);
    }, 300);
  }, [componentId]);

  const handleBack = (): void => {
    if (onBack) {
      onBack();
    } else {
      window.location.hash = '#preview/browse/search/generic';
    }
  };

  const handleDownload = (asset: GenericAsset): void => {
    window.open(asset.downloadUrl, '_blank');
  };

  // Loading state
  if (loading) {
    return (
      <div className="generic-detail-page generic-detail-page--loading">
        <div className="generic-detail-page__loading-spinner" />
        <p>Loading component details...</p>
      </div>
    );
  }

  // Error state
  if (error || !component) {
    return (
      <div className="generic-detail-page generic-detail-page--error">
        <p className="generic-detail-page__error-text">{error || 'Component not found'}</p>
        <button
          type="button"
          className="generic-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
      </div>
    );
  }

  const formatConfig = FORMAT_CONFIG[component.format] || { label: component.format, color: 'gray' };

  return (
    <div className="generic-detail-page">
      {/* Header with back button */}
      <header className="generic-detail-page__header">
        <button
          type="button"
          className="generic-detail-page__back-btn"
          onClick={handleBack}
        >
          ← Back to Search
        </button>
      </header>

      {/* Component info card */}
      <section className="generic-detail-page__info">
        <div className="generic-detail-page__title-row">
          <h1 className="generic-detail-page__title">{component.displayName}</h1>
          <span 
            className={`generic-detail-page__format-badge generic-detail-page__format-badge--${formatConfig.color}`}
          >
            {formatConfig.label}
          </span>
        </div>

        <div className="generic-detail-page__metadata">
          <div className="generic-detail-page__metadata-item">
            <span className="generic-detail-page__metadata-label">Version</span>
            <span className="generic-detail-page__metadata-value">{component.version}</span>
          </div>
          {component.group && (
            <div className="generic-detail-page__metadata-item">
              <span className="generic-detail-page__metadata-label">Group</span>
              <span className="generic-detail-page__metadata-value">{component.group}</span>
            </div>
          )}
          <div className="generic-detail-page__metadata-item">
            <span className="generic-detail-page__metadata-label">Name</span>
            <span className="generic-detail-page__metadata-value">{component.name}</span>
          </div>
          <div className="generic-detail-page__metadata-item">
            <span className="generic-detail-page__metadata-label">Repository</span>
            <span className="generic-detail-page__metadata-value">{component.repository}</span>
          </div>
        </div>
      </section>

      {/* Assets list */}
      <section className="generic-detail-page__assets">
        <h2 className="generic-detail-page__section-title">
          Assets ({component.assets.length})
        </h2>

        {component.assets.length === 0 ? (
          <p className="generic-detail-page__no-assets">No assets available</p>
        ) : (
          <table className="generic-detail-page__assets-table">
            <thead>
              <tr>
                <th>Path</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {component.assets.map((asset) => (
                <tr key={asset.id}>
                  <td className="generic-detail-page__asset-path">
                    {asset.path}
                  </td>
                  <td className="generic-detail-page__asset-actions">
                    <button
                      type="button"
                      className="generic-detail-page__download-btn"
                      onClick={() => handleDownload(asset)}
                      title="Download asset"
                    >
                      ⬇ Download
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

export default GenericDetailPage;


