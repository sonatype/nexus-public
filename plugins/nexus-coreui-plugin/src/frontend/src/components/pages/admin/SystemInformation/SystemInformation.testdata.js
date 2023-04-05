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
export const singleNodeResponse1 = {
  'nexus-status': {
    status: 'value1'
  },
  'nexus-node': {
    'node-id': 'nodeId1'
  },
  'nexus-configuration': {
    enabled: true
  },
  'nexus-properties': {
    property: false
  },
  'nexus-license': {
    fingerprint: 'hash'
  },
  'system-time': {
    time: 'value'
  },
  'system-properties': {
    property: 'value1'
  },
  'system-environment': {
    property: 'value1'
  },
  'system-runtime': {
    property: 'value1'
  },
  'system-network': {
    en0: {
      enabled: true
    }
  },
  'system-filestores': {
    '/dev/null': {
      path: '/dev/null'
    }
  }
};

export const singleNodeResponse2 = {
  'nexus-status': {
    status: 'value2'
  },
  'nexus-node': {
    'node-id': 'nodeId2'
  },
  'nexus-configuration': {
    enabled: true
  },
  'nexus-properties': {
    property: true
  },
  'nexus-license': {
    fingerprint: 'hash'
  },
  'system-time': {
    time: 'value'
  },
  'system-properties': {
    property: 'value2'
  },
  'system-environment': {
    property: 'value2'
  },
  'system-runtime': {
    property: 'value2'
  },
  'system-network': {
    en0: {
      enabled: true
    }
  },
  'system-filestores': {
    '/dev/null': {
      path: '/dev/null'
    }
  }
};

export const multiNodeResponse = {
  'nexus-status': {
    nodeId1: {
      status: 'value1'
    },
    nodeId2: {
      status: 'value2'
    }
  },
  'nexus-node': {
    nodeId1: {
      'node-id': 'nodeId1'
    },
    nodeId2: {
      'node-id': 'nodeId2'
    }
  },
  'nexus-configuration': {
    nodeId1: {
      enabled: true
    },
    nodeId2: {
      enabled: true
    }
  },
  'nexus-properties': {
    nodeId1: {
      property: false
    },
    nodeId2: {
      property: true
    }
  },
  'nexus-license': {
    nodeId1: {
      fingerprint: 'hash'
    },
    nodeId2: {
      fingerprint: 'hash'
    }
  },
  'system-time': {
    nodeId1: {
      time: 'value'
    },
    nodeId2: {
      time: 'value'
    }
  },
  'system-properties': {
    nodeId1: {
      property: 'value1'
    },
    nodeId2: {
      property: 'value2'
    }
  },
  'system-environment': {
    nodeId1: {
      property: 'value1'
    },
    nodeId2: {
      property: 'value2'
    }
  },
  'system-runtime': {
    nodeId1: {
      property: 'value1'
    },
    nodeId2: {
      property: 'value2'
    }
  },
  'system-network': {
    nodeId1: {
      en0: {
        enabled: true
      }
    },
    nodeId2: {
      en0: {
        enabled: true
      }
    }
  },
  'system-filestores': {
    nodeId1: {
      '/dev/null': {
        path: '/dev/null'
      }
    },
    nodeId2: {
      '/dev/null': {
        path: '/dev/null'
      }
    }
  }
};
