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

import React, { useState, useEffect } from 'react';
import type { RawResult } from './raw.types';
import { mockRawDetailApi } from './mockData';
import './RawDetailPage.scss';

export interface RawDetailPageProps {
  assetId: string;
  onBack?: () => void;
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '-';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

/**
 * Detail page for a raw file.
 */
export function RawDetailPage({ assetId, onBack }: RawDetailPageProps): React.ReactElement {
  const [detail, setDetail] = useState<RawResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    async function loadDetail() {
      setLoading(true);
      setError(undefined);
      try {
        const data = await mockRawDetailApi(assetId);
        setDetail(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load details');
      } finally {
        setLoading(false);
      }
    }
    loadDetail();
  }, [assetId]);

  if (loading) {
    return (
      <div className="raw-detail-page raw-detail-page--loading">
        <div className="raw-detail-page__spinner" />
        Loading...
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="raw-detail-page raw-detail-page--error">
        <span>⚠️ {error || 'File not found'}</span>
        {onBack && (
          <button type="button" onClick={onBack} className="raw-detail-page__back-button">
            Back to Search
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="raw-detail-page">
      <div className="raw-detail-page__header">
        {onBack && (
          <button type="button" onClick={onBack} className="raw-detail-page__back-button">
            ← Back
          </button>
        )}
        <h1 className="raw-detail-page__title">{detail.name}</h1>
        <p className="raw-detail-page__path">{detail.path}</p>
      </div>

      <div className="raw-detail-page__content">
        <section className="raw-detail-page__section">
          <h2 className="raw-detail-page__section-title">File Information</h2>
          <dl className="raw-detail-page__info-grid">
            <dt>Repository</dt>
            <dd>{detail.repository}</dd>
            <dt>Content Type</dt>
            <dd>{detail.contentType || '-'}</dd>
            <dt>Size</dt>
            <dd>{formatFileSize(detail.size)}</dd>
            <dt>Last Modified</dt>
            <dd>{detail.lastModified || '-'}</dd>
          </dl>
        </section>

        {detail.checksums && (
          <section className="raw-detail-page__section">
            <h2 className="raw-detail-page__section-title">Checksums</h2>
            <dl className="raw-detail-page__info-grid">
              {detail.checksums.sha256 && (
                <>
                  <dt>SHA-256</dt>
                  <dd className="raw-detail-page__checksum">{detail.checksums.sha256}</dd>
                </>
              )}
              {detail.checksums.sha1 && (
                <>
                  <dt>SHA-1</dt>
                  <dd className="raw-detail-page__checksum">{detail.checksums.sha1}</dd>
                </>
              )}
              {detail.checksums.md5 && (
                <>
                  <dt>MD5</dt>
                  <dd className="raw-detail-page__checksum">{detail.checksums.md5}</dd>
                </>
              )}
            </dl>
          </section>
        )}

        {detail.downloadUrl && (
          <section className="raw-detail-page__section">
            <h2 className="raw-detail-page__section-title">Download</h2>
            <a
              href={detail.downloadUrl}
              className="raw-detail-page__download-button"
              download
            >
              Download File
            </a>
          </section>
        )}
      </div>
    </div>
  );
}

export default RawDetailPage;


