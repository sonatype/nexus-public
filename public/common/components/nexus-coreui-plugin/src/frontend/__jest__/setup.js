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
require('@testing-library/jest-dom');
const { configure } = require('@testing-library/react');
const { ExtJS } = require('@sonatype/nexus-ui-plugin');

// Increase default async timeout for findBy/waitFor queries.
// CI machines are CPU-constrained (Maven runs 3 threads), so the default
// 1000ms timeout causes flaky failures on navigation/render-heavy tests.
configure({ asyncUtilTimeout: 5000 });

jest.mock('axios', () => ({
  ...jest.requireActual('axios'), // Use most functions from actual axios
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
  all: jest.fn().mockImplementation((requests) => Promise.all(requests))
}));

let lastValue = 0;

window.crypto = {
  getRandomValues: function (buffer) {
    buffer.fill(lastValue++);
  }
};

window.plugins = [];
window.BlobStoreTypes = {};
window.HTMLElement.prototype.scrollIntoView = jest.fn();
window.ResizeObserver = jest.fn().mockReturnValue({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn()
});

global.Ext = {
  getApplication: jest.fn().mockReturnValue({
    getStore: jest.fn().mockReturnValue({
      on: jest.fn(),
      un: jest.fn()
    }),
    getController: jest.fn().mockReturnValue({
      on: jest.fn(),
      un: jest.fn()
    })
  }),
  // keep it async so it's closer to real world behavior, but no need to wait longer than we have to
  defer: (callback) => setTimeout(callback, 0),
  isEmpty: jest.fn().mockImplementation((value) => {
    if (value === undefined || value === null) {
      return true;
    } else if (typeof value?.length === 'number') {
      return value.length === 0;
    } else {
      return false;
    }
  }),
  widget: jest.fn()
};

global.NX = {
  Messages: {
    success: jest.fn(),
    error: jest.fn()
  },
  Permissions: {
    check: jest.fn(),
    permissions: {},
  },
  getApplication: jest.fn(() => ({})),
  app: {
    Application: {
      bundleActive: jest.fn()
    }
  },
  State: {
    getValue: jest.fn((key) => {
      // Default hosted repository evaluation to true
      if (key === 'hostedRepositoryEvaluationEnabled') {
        return true;
      }
      return undefined;
    }),
    getUser: jest.fn(),
    getEdition: jest.fn()
  },
  Security: {
    hasUser: jest.fn()
  }
};

// Add missing getBoundingClientRect
Range.prototype.getBoundingClientRect = jest.fn(() => {
  return {
    width: 100,
    height: 100,
    top: 100,
    left: 100,
    bottom: 100,
    right: 100
  };
});

const getHeight = (el) => {
  const isContainer = el.classList.contains('nx-transfer-list__item-list'),
    isItem = el.classList.contains('nx-transfer-list__item'),
    height = isContainer ? 520 : isItem ? 40 : 0;
  return height;
};

Element.prototype.getBoundingClientRect = jest.fn().mockImplementation(function () {
  const height = getHeight(this),
    siblingItems = this.parentElement?.children ?? [],
    idx = Array.prototype.indexOf.call(siblingItems, this),
    top = 40 * idx;

  return {
    bottom: 0,
    height,
    left: 0,
    right: 0,
    top,
    width: 0
  };
});

jest.spyOn(Element.prototype, 'clientHeight', 'get').mockImplementation(function () {
  return getHeight(this);
});

// Prevent App.jsx module-level bootstrapAndRender() from creating a rogue React root in tests.
// Tests render <App /> directly; the module-level waitForExtJs boot path is not needed.
jest.spyOn(ExtJS, 'waitForExtJs').mockImplementation(() => {});

jest.mock('swagger-ui-react', () => {
  return jest.fn().mockReturnValue(null);
});

jest.mock('@fortawesome/react-fontawesome', () => ({
  FontAwesomeIcon: ({icon, ...props}) => {
    const iconName = typeof icon === 'string' ? icon : (icon?.iconName || 'icon');
    return require('react').createElement('span', {'data-testid': `fa-icon-${iconName}`, ...props});
  }
}));
