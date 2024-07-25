/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import axios from 'axios';

export default function configureAxios() {
  // Configure axios
  axios.defaults.xsrfCookieName = 'NX-ANTI-CSRF-TOKEN';
  axios.defaults.xsrfHeaderName = 'NX-ANTI-CSRF-TOKEN';
  axios.defaults.baseURL = NX.app.relativePath;
  axios.defaults.headers.common['X-Nexus-UI'] = true;
  const axiosAdapter = axios.defaults.adapter;
  if (typeof axiosAdapter === 'function') {
    axios.defaults.adapter = function(config) {
      // Generate a new cache buster for each request
      const timestamp = new Date().getTime();
      if (config.url.indexOf('?') !== -1) {
        config.url += '&_dc=' + timestamp;
      }
      else {
        config.url += '?_dc=' + timestamp;
      }

      return axiosAdapter(config);
    };
  }
}
