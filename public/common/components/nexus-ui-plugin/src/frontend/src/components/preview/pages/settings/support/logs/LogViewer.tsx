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

import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Download, Stamp, Loader2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsButton,
  SettingsTextInput,
  SettingsSelect,
  SettingsAlert,
} from '../../../../shared/form';
import { useLogsApi } from './useLogsApi';
import { REFRESH_RATES, LOG_SIZES } from './types';

import './LogViewer.scss';

interface LogViewerProps {
  filename: string;
  onBack?: () => void;
}

/**
 * LogViewer - Displays log file content with auto-refresh and mark insertion
 */
export function LogViewer({ filename, onBack }: LogViewerProps) {
  const [logContent, setLogContent] = useState('');
  const [refreshPeriod, setRefreshPeriod] = useState(0);
  const [logSize, setLogSize] = useState(25);
  const [mark, setMark] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [contentError, setContentError] = useState<string | null>(null);

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const intervalRef = useRef<number | null>(null);

  const { fetchLogContent, insertMark, getDownloadUrl, loading, error, setError } = useLogsApi();

  // filename comes from the route param (raw: true) and is already unencoded
  const isNexusLog = filename === 'nexus.log';
  const canUpdate = ExtJS.checkPermission('nexus:logging:update');

  // Fetch log content
  const loadContent = useCallback(async () => {
    try {
      setContentError(null);
      const bytesCount = logSize * -1024; // Negative to get last N bytes
      const content = await fetchLogContent(filename, bytesCount);
      setLogContent(content);
    } catch (err: any) {
      setContentError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [filename, logSize, fetchLogContent]);

  // Initial load
  useEffect(() => {
    setIsLoading(true);
    loadContent();
  }, [loadContent]);

  // Setup auto-refresh
  useEffect(() => {
    // Clear existing interval
    if (intervalRef.current) {
      window.clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    // Setup new interval if period > 0
    if (refreshPeriod > 0) {
      intervalRef.current = window.setInterval(() => {
        loadContent();
      }, refreshPeriod * 1000);
    }

    return () => {
      if (intervalRef.current) {
        window.clearInterval(intervalRef.current);
      }
    };
  }, [refreshPeriod, loadContent]);

  // Scroll to bottom when content changes
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.scrollTop = textareaRef.current.scrollHeight;
    }
  }, [logContent]);

  // Handle mark insertion
  const handleInsertMark = useCallback(async () => {
    if (!mark.trim()) return;
    
    try {
      await insertMark(mark || 'MARK');
      setMark('');
      // Refresh log content to show the mark
      await loadContent();
    } catch (err) {
      // Error is handled by the hook
    }
  }, [mark, insertMark, loadContent]);

  // Handle Enter key on mark input
  const handleMarkKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (event.key === 'Enter') {
        event.preventDefault();
        handleInsertMark();
      }
    },
    [handleInsertMark]
  );

  // Handle download
  const handleDownload = useCallback(() => {
    const url = ExtJS.urlOf(getDownloadUrl(filename));
    ExtJS.downloadUrl(url);
  }, [filename, getDownloadUrl]);

  // Handle refresh period change
  const handleRefreshPeriodChange = useCallback((value: string) => {
    setRefreshPeriod(Number(value));
  }, []);

  // Handle log size change
  const handleLogSizeChange = useCallback((value: string) => {
    setLogSize(Number(value));
  }, []);

  return (
    <Box className="log-viewer">
      {/* Header with title and download button */}
      <Flex justify="between" align="center" className="log-viewer__header">
        <Text weight="medium" size="3" className="log-viewer__title">
          Viewing {filename}
        </Text>
        <SettingsButton variant="primary" onClick={handleDownload} icon={Download}>
          Download
        </SettingsButton>
      </Flex>

      {/* Toolbar */}
      <Box className="log-viewer__toolbar">
        <Flex align="center" justify="between" gap="4" wrap="wrap">
          {/* Mark insertion (only for nexus.log with update permission) */}
          {isNexusLog && canUpdate && (
            <Flex align="center" gap="2" className="log-viewer__mark-section">
              <Text size="2" className="log-viewer__label">
                Marker to insert:
              </Text>
              <SettingsTextInput
                value={mark}
                onChange={setMark}
                placeholder="MARK"
                className="log-viewer__mark-input"
                onKeyDown={handleMarkKeyDown}
              />
              <SettingsButton
                variant="secondary"
                onClick={handleInsertMark}
                disabled={loading}
                icon={Stamp}
              >
                Insert
              </SettingsButton>
            </Flex>
          )}

          <Flex align="center" gap="4" className="log-viewer__options">
            {/* Refresh rate */}
            <Flex align="center" gap="2">
              <Text size="2" className="log-viewer__label">
                Refresh Rate:
              </Text>
              <SettingsSelect
                value={String(refreshPeriod)}
                onChange={handleRefreshPeriodChange}
                options={REFRESH_RATES.map((r) => ({
                  value: String(r.value),
                  label: r.label,
                }))}
                className="log-viewer__select"
              />
            </Flex>

            {/* Log size */}
            <Flex align="center" gap="2">
              <Text size="2" className="log-viewer__label">
                Size:
              </Text>
              <SettingsSelect
                value={String(logSize)}
                onChange={handleLogSizeChange}
                options={LOG_SIZES.map((s) => ({
                  value: String(s.value),
                  label: s.label,
                }))}
                className="log-viewer__select"
              />
            </Flex>
          </Flex>
        </Flex>
      </Box>

      {/* Error alerts */}
      {(error || contentError) && (
        <Box mb="3">
          <SettingsAlert
            type="error"
            onDismiss={() => {
              setError(null);
              setContentError(null);
            }}
          >
            {error || contentError}
          </SettingsAlert>
        </Box>
      )}

      {/* Log content */}
      <Box className="log-viewer__content">
        {isLoading ? (
          <Flex align="center" justify="center" className="log-viewer__loading">
            <Loader2 size={20} className="log-viewer__spinner" />
            <Text size="2" ml="2">
              Loading log content...
            </Text>
          </Flex>
        ) : (
          <textarea
            ref={textareaRef}
            className="log-viewer__textarea"
            value={logContent}
            readOnly
            aria-label={`Log content for ${filename}`}
          />
        )}
      </Box>
    </Box>
  );
}

export default LogViewer;


