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
/*
 * /js/repoServer/referenceData.js Reference service data objects
 * used for reading form data and compiling resource state objects to send back
 * to the service.
 */

/*global NX*/
NX.define('Sonatype.repoServer.referenceData', {
  singleton : true,

  requirejs : ['Sonatype/all'],

  globalSettingsState : {
    securityAnonymousUsername : "",
    securityAnonymousPassword : "",
    securityEnabled : false,
    securityAnonymousAccessEnabled : false,
    securityRealms : [],
    smtpSettings : {
      host : "",
      port : 25,
      username : "",
      password : "",
      systemEmailAddress : "",
      sslEnabled : "",
      tlsEnabled : ""
    },
    globalConnectionSettings : {
      connectionTimeout : 0,
      retrievalRetryCount : 0,
      queryString : "",
      userAgentString : ""
    },
    remoteProxySettings : {
      httpProxySettings : {
        proxyHostname : "",
        proxyPort : 0,
        authentication : {
          username : "",
          password : "",
          ntlmHost : "",
          ntlmDomain : ""
        }
      },
      httpsProxySettings : {
        proxyHostname : "",
        proxyPort : 0,
        authentication : {
          username : "",
          password : "",
          ntlmHost : "",
          ntlmDomain : ""
        }
      },
      nonProxyHosts : []
    },
    globalRestApiSettings : {
      baseUrl : "",
      forceBaseUrl : false,
      uiTimeout : 30000
    },
    systemNotificationSettings : {
      enabled : false,
      emailAddresses : "",
      roles : []
    }
  },

  repositoryState : {
    virtual : {
      repoType : "",
      id : "",
      name : "",
      shadowOf : "",
      provider : "",
      providerRole : "",
      syncAtStartup : false,
      exposed : true
    },

    hosted : {
      repoType : "",
      id : "",
      name : "",
      writePolicy : "ALLOW_WRITE_ONCE",
      browseable : true,
      indexable : true,
      exposed : true,
      notFoundCacheTTL : 0,
      repoPolicy : "",
      provider : "",
      providerRole : "",
      overrideLocalStorageUrl : "",
      defaultLocalStorageUrl : "",
      downloadRemoteIndexes : true,
      checksumPolicy : ""
    },

    proxy : {
      repoType : "",
      id : "",
      name : "",
      browseable : true,
      indexable : true,
      notFoundCacheTTL : 0,
      artifactMaxAge : 0,
      metadataMaxAge : 0,
      itemMaxAge : 0,
      repoPolicy : "",
      provider : "",
      providerRole : "",
      overrideLocalStorageUrl : "",
      defaultLocalStorageUrl : "",
      downloadRemoteIndexes : true,
      autoBlockActive : true,
      fileTypeValidation : false,
      exposed : true,
      checksumPolicy : "",
      remoteStorage : {
        remoteStorageUrl : "",
        authentication : {
          username : "",
          password : "",
          ntlmHost : "",
          ntlmDomain : ""
        },
        connectionSettings : {
          connectionTimeout : 0,
          retrievalRetryCount : 0,
          queryString : "",
          userAgentString : ""
        }
      }
    } // end repositoryProxyState
  },

  group : {
    id : "",
    name : "",
    format : "",
    exposed : "",
    provider : "",
    repositories : []
    // note: internal record structure is the responsibility of data modifier
    // func
    // {
    // id:"central",
    // name:"Maven Central",
    // resourceURI:".../repositories/repoId" // added URI to be able to reach
    // repo
    // }
  },

  route : {
    id : "",
    ruleType : "",
    groupId : "",
    pattern : "",
    repositories : []
    // @todo: there's a discrepancy between routes list and state
    // representation of
    // the repo data inside routes data
  },

  schedule : {
    manual : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ]
    },
    once : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      startDate : "",
      startTime : ""
    },
    hourly : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      startDate : "",
      startTime : ""
    },
    daily : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      startDate : "",
      recurringTime : ""
    },
    weekly : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      startDate : "",
      recurringTime : "",
      recurringDay : []
    },
    monthly : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      startDate : "",
      recurringTime : "",
      recurringDay : []
    },
    advanced : {
      id : "",
      name : "",
      enabled : "",
      typeId : "",
      alertEmail : "",
      schedule : "",
      properties : [
        {
          id : "",
          value : ""
        }
      ],
      cronCommand : ""
    }
  },

  upload : {
    r : "",
    g : "",
    a : "",
    v : "",
    p : "",
    c : "",
    e : ""
  },

  users : {
    userId : "",
    firstName : "",
    lastName : "",
    email : "",
    status : "",
    roles : []
  },

  userNew : {
    userId : "",
    firstName : "",
    lastName : "",
    email : "",
    status : "",
    password : "",
    roles : []
  },

  roles : {
    id : "",
    name : "",
    description : "",
    sessionTimeout : 0,
    roles : [],
    privileges : []
  },

  privileges : {
    target : {
      name : "",
      description : "",
      type : "",
      repositoryTargetId : "",
      repositoryId : "",
      repositoryGroupId : "",
      method : []
    }
  },

  repoTargets : {
    id : "",
    name : "",
    contentClass : "",
    patterns : []
  },

  contentClasses : {
    contentClass : "",
    name : ""
  },

  repoMirrors : [
    {
      id : "",
      url : ""
    }
  ]

});

