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

import { useCallback, useMemo, useRef, useEffect } from 'react';
import { useMachine } from '@xstate/react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { logViewerMachine } from './LogViewerMachine';
import { LOGS_API } from './types';

export interface UseLogViewerReturn {
  logContent: string;
  isLoading: boolean;
  error: string | null;
  mark: string;
  refreshPeriod: number;
  logSize: number;
  setMark: (value: string) => void;
  setRefreshPeriod: (value: string) => void;
  setLogSize: (value: string) => void;
  handleInsertMark: () => void;
  handleDownload: () => void;
  textareaRef: React.RefObject<HTMLTextAreaElement>;
}

export function useLogViewer(filename: string): UseLogViewerReturn {
  const machine = useMemo(
    () => logViewerMachine.withContext({ ...logViewerMachine.context, filename }),
    [filename]
  );

  const [state, send] = useMachine(machine);
  const { logContent, refreshPeriod, logSize, mark, error } = state.context;

  const isLoading = state.matches('loading') || state.matches('refreshing');

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Scroll to bottom when content changes — DOM side effect, stays in hook
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.scrollTop = textareaRef.current.scrollHeight;
    }
  }, [logContent]);

  const setMark = useCallback((value: string) => {
    send({ type: 'SET_MARK', value });
  }, [send]);

  const setRefreshPeriod = useCallback((value: string) => {
    send({ type: 'SET_REFRESH_PERIOD', value: Number(value) });
  }, [send]);

  const setLogSize = useCallback((value: string) => {
    send({ type: 'SET_LOG_SIZE', value: Number(value) });
  }, [send]);

  const handleInsertMark = useCallback(() => {
    if (!mark.trim()) return;
    send({ type: 'INSERT_MARK' });
  }, [mark, send]);

  const handleDownload = useCallback(() => {
    const url = ExtJS.urlOf(LOGS_API.VIEW(filename));
    ExtJS.downloadUrl(url);
  }, [filename]);

  return {
    logContent,
    isLoading,
    error,
    mark,
    refreshPeriod,
    logSize,
    setMark,
    setRefreshPeriod,
    setLogSize,
    handleInsertMark,
    handleDownload,
    textareaRef,
  };
}
