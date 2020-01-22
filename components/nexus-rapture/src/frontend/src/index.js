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
import Axios from 'axios';
import React from 'react';
import ReactDOM from 'react-dom';
import * as xstate from 'xstate';

import registerFeature from './registerFeature';

Axios.defaults.xsrfCookieName = Axios.defaults.xsrfHeaderName = 'NX-ANTI-CSRF-TOKEN';

window.axios = Axios;
window.react = React;
window.ReactDOM = ReactDOM;
window.xstate = xstate;

// Declare an inital (empty) array for plugin configurations
window.plugins = [];

// A function for the ExtJS codebase to call to register React plugins
window.onStart = function() {
  window.plugins.forEach((plugin) => {
    if (plugin.features) {
      plugin.features.forEach(registerFeature);
    }
  })
};
