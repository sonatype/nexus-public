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
export default {
  MALICIOUS_RISK: {
    MENU:{
      text:'Malicious Risk <span class="nxrm-new-tag">NEW</span>',
      description: 'Visualize risk in your repositories'
    },
    TITLE: 'Malicious Risk Dashboard',
    COMPONENT_MALWARE: {
      MALICIOUS_COMPONENTS: {
        TEXT: 'Malicious Components are Malware',
        DESCRIPTION: 'Malicious components exploit the open source DevOps tool chain to introduce malware such as ' +
            'credential harvester, crypto-miner, a virus, ransomware, data corruption, malicious code injector, etc.'
      },
      AVERAGE_ATTACK: {
        TEXT: 'Average Cost to Remediate a Malicious Attack',
        DESCRIPTION: '$5.12 million'
      },
      LEARN_MORE: {
        TEXT: 'Learn More',
        URL: 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/press-releases'
      }
    },
    LOAD_ERROR: 'An error occurred while fetching the malicious risk data',
  }
}
