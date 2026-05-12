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

import React, {createContext, useContext, useEffect, useState, useCallback} from 'react';
import {isExtJSLoaded, onExtJSLoad} from '../utils/extJsLoader';

/**
 * StateContext provides application state management.
 *
 * Phase 1: React Shell - This context bridges React and ExtJS state.
 * When ExtJS is loaded, it delegates to NX.State.
 * When ExtJS is not loaded, returns default values.
 *
 * Future phases will manage state independently from ExtJS.
 */

const StateContext = createContext({
  getValue: () => null,
  setValue: () => {},
  useState: () => null,
  getEdition: () => 'OSS',
  getVersion: () => null,
  getLicense: () => null
});

export function StateProvider({ children }) {
  const [stateReady, setStateReady] = useState(isExtJSLoaded());

  useEffect(() => {
    onExtJSLoad(() => {
      setStateReady(true);
    });
  }, []);

  const getValue = useCallback((key) => {
    if (isExtJSLoaded() && window.NX?.State?.getValue) {
      return window.NX.State.getValue(key);
    }
    // Return null when ExtJS not loaded
    return null;
  }, [stateReady]);

  const setValue = useCallback((key, value) => {
    if (isExtJSLoaded() && window.NX?.State?.setValue) {
      window.NX.State.setValue(key, value);
    }
  }, [stateReady]);

  // Hook for reactive state values
  const useStateValue = (key) => {
    const [value, setValue] = useState(() => getValue(key));

    useEffect(() => {
      if (!isExtJSLoaded()) {
        // Wait for ExtJS to load
        onExtJSLoad(() => {
          setValue(getValue(key));
        });
        return;
      }

      function handleChange() {
        const newValue = getValue(key);
        if (value !== newValue) {
          setValue(newValue);
        }
      }

      const state = window.Ext.getApplication().getStore('State');
      state.on('datachanged', handleChange);
      return () => state.un('datachanged', handleChange);
    }, [key, value]);

    return value;
  };

  const getEdition = useCallback(() => {
    if (isExtJSLoaded() && window.NX?.State?.getEdition) {
      return window.NX.State.getEdition();
    }
    return 'OSS'; // Default to OSS edition
  }, [stateReady]);

  const getVersion = useCallback(() => {
    if (isExtJSLoaded() && window.NX?.State?.getVersion) {
      return window.NX.State.getVersion();
    }
    return getValue('status')?.version || null;
  }, [stateReady]);

  const getLicense = useCallback(() => {
    return getValue('license');
  }, [getValue]);

  const value = {
    getValue,
    setValue,
    useState: useStateValue,
    getEdition,
    getVersion,
    getLicense
  };

  return (
    <StateContext.Provider value={value}>
      {children}
    </StateContext.Provider>
  );
}

export function useAppState() {
  const context = useContext(StateContext);
  if (!context) {
    throw new Error('useAppState must be used within a StateProvider');
  }
  return context;
}

export default StateContext;
