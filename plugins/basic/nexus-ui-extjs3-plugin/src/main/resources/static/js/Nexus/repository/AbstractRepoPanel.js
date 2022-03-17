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
/*global define*/
define('Nexus/repository/AbstractRepoPanel',['extjs', 'Sonatype/all', 'nexus', 'Nexus/repository/action'], function(Ext, Sonatype, Nexus, Action){

/*
 * Repository panel superclass
 *
 * config options: { id: the is of this panel instance [required] title: title
 * of this panel (shows in tab) }
 */
Sonatype.repoServer.AbstractRepoPanel = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {},
        sp = Sonatype.lib.Permissions;

  Ext.apply(this, config, defaultConfig);

  this.ctxRecord = null;
  this.reposGridPanel = null;

  this.repoActions = {
    downloadRemoteItem : new Action({
      condition : function(repo, content) {
        return content && content.isLeaf() && repo.repoType === 'proxy';
      },
      action : {
        text : 'Download From Remote',
        scope : this,
        handler : this.downloadFromRemoteHandler
      }}),
    downloadItem : new Action({
      condition : function(repo, content) {
        return content && content.isLeaf();
      },
      action : {
        text : 'Download',
        scope : this,
        handler : this.downloadHandler
      }
    }),
    viewRemote : new Action(
          {
            condition : function(repo, content) {
              return content && !content.isRoot && !content.isLeaf() &&
                    repo.repoType === 'proxy';
            },
            action : {
              text : 'View Remote',
              scope : this,
              handler : this.downloadFromRemoteHandler
            }}),
    clearCache : new Action({
      condition : function(repo, content) {
        return sp.checkPermission('nexus:cache', sp.DELETE) && repo.repoType !== 'virtual' && repo.userManaged;
      },
      action : {
        text : 'Expire Cache',
        handler : this.clearCacheHandler,
        scope : this
      }
    }),
    rebuildMetadata : new Action({
      condition : function(repo) {
        return sp.checkPermission('nexus:metadata', sp.DELETE) &&
              (repo.format === 'maven2' || repo.format === 'maven1') &&
              (repo.repoType === 'hosted' || repo.repoType === 'group') && repo.userManaged;
      },
      action : {
        text : 'Rebuild Metadata',
        handler : this.rebuildMetadataHandler,
        scope : this
      }
    }),
    putInService : new Action({
      condition : function(repo) {
        return sp.checkPermission('nexus:repostatus', sp.EDIT) &&
              repo.repoType !== 'group' &&
              repo.status && repo.status.localStatus !== 'IN_SERVICE';
      },
      action : {
        text : 'Put in Service',
        scope : this,
        handler : this.putInServiceHandler
      }
    }),
    putOutOfService : new Action({
      condition : function(repo) {
        return sp.checkPermission('nexus:repostatus', sp.EDIT) &&
              repo.repoType !== 'group' &&
              repo.status && repo.status.localStatus === 'IN_SERVICE';
      },
      action : {
        text : 'Put Out of Service',
        scope : this,
        handler : this.putOutOfServiceHandler
      }
    }),
    allowProxy : new Action({
      condition : function(repo) {
        return sp.checkPermission('nexus:repostatus', sp.EDIT) &&
              repo.repoType === 'proxy' && repo.status && repo.status.proxyMode !== 'ALLOW';
      },
      action : {
        text : 'Allow Proxy',
        scope : this,
        handler : this.proxyStatusHandlerFactory('ALLOW', 'ALLOW')
      }
    }),
    blockProxy : new Action({
      condition : function(repo) {
        return sp.checkPermission('nexus:repostatus', sp.EDIT) &&
              repo.repoType === 'proxy' && repo.status && repo.status.proxyMode === 'ALLOW';
      },
      action : {
        text : 'Block Proxy',
        scope : this,
        handler : this.proxyStatusHandlerFactory('BLOCKED_MANUAL', 'BLOCKED')
      }
    }),
    deleteRepoItem : new Action({
      condition : function(repo) {
        return true;
      },
      action : {
        text : 'Delete',
        scope : this,
        handler : this.deleteRepoItemHandler
      }
    })
  };

  Sonatype.repoServer.AbstractRepoPanel.superclass.constructor.call(this, {});
};

Ext.extend(Sonatype.repoServer.AbstractRepoPanel, Ext.Panel, {
      hasSelection : function() {
        return this.ctxRecord || this.reposGridPanel.getSelectionModel().hasSelection();
      },

  /*
      viewHandler : function() {
        if (this.ctxRecord || this.reposGridPanel.getSelectionModel().hasSelection())
        {
          var rec = this.ctxRecord || this.reposGridPanel.getSelectionModel().getSelected();
          this.viewRepo(rec);
        }
      },
      */

      repoActionAjaxSuccessHandler : function(response, options) {
        var statusResp = Ext.decode(response.responseText);
        this.updateRepoStatuses(statusResp.data, options.repoRecord);
      },

      repoActionAjaxFailureHandlerFactory : function(msg) {
        return function(response, options) {
          Sonatype.utils.connectionError(response, msg);
        };
      },

      clearCacheHandler : function(rec) {
        var url = Sonatype.config.repos.urls.cache + rec.data.resourceURI.slice(Sonatype.config.host.length + Sonatype.config.servicePath.length);

        // make sure to provide /content path for repository root requests like
        // ../repositories/central
        if (/.*\/repositories\/[^\/]*$/i.test(url) || /.*\/repo_groups\/[^\/]*$/i.test(url))
        {
          url += '/content';
        }

        Ext.Ajax.request({
              url : url,
              failure : this.repoActionAjaxFailureHandlerFactory("The server did not clear the repository's cache."),
              scope : this,
              method : 'DELETE'
            });
      },

      rebuildMetadataHandler : function(rec) {
        var url = Sonatype.config.repos.urls.metadata + rec.data.resourceURI.slice(Sonatype.config.host.length + Sonatype.config.servicePath.length);

        // make sure to provide /content path for repository root requests like
        // ../repositories/central
        if (/.*\/repositories\/[^\/]*$/i.test(url) || /.*\/repo_groups\/[^\/]*$/i.test(url))
        {
          url += '/content';
        }

        Ext.Ajax.request({
              url : url,
              failure :  this.repoActionAjaxFailureHandlerFactory('The server did not rebuild metadata in the repository.'),
              scope : this,
              method : 'DELETE'
            });
      },

      putInServiceHandler : function(rec) {
        Ext.Ajax.request({
              url : rec.data.resourceURI + '/status',
              jsonData : {
                data : {
                  id : rec.data.id,
                  repoType : rec.data.repoType,
                  localStatus : 'IN_SERVICE'
                }
              },
              success : this.repoActionAjaxSuccessHandler,
              failure : this.repoActionAjaxFailureHandlerFactory('The server did not put the repository into service.'),
              repoRecord : rec,
              scope : this,
              method : 'PUT'
            });
      },

      putOutOfServiceHandler : function(rec) {
        Ext.Ajax.request({
              url : rec.data.resourceURI + '/status',
              jsonData : {
                data : {
                  id : rec.data.id,
                  repoType : rec.data.repoType,
                  localStatus : 'OUT_OF_SERVICE'
                }
              },
              success : this.repoActionAjaxSuccessHandler,
              failure : this.repoActionAjaxFailureHandlerFactory('The server did not put the repository out of service.'),
              repoRecord : rec,
              scope : this,
              method : 'PUT'
            });
      },

      proxyStatusHandlerFactory : function(state, stateName) {
        return function(rec) {
          if (rec.data.status) {
            Ext.Ajax.request({
              url : rec.data.resourceURI + '/status',
              jsonData : {
                data : {
                  id : rec.data.id,
                  repoType : rec.data.repoType,
                  localStatus : rec.data.status.localStatus,
                  remoteStatus : rec.data.status.remoteStatus,
                  proxyMode : state
                }
              },
              success : this.repoActionAjaxSuccessHandler,
              failure : this.repoActionAjaxFailureHandlerFactory('The server did not update the proxy repository status to ' + stateName + '.'),
              scope : this,
              repoRecord : rec,
              method : 'PUT'
            });
          }
        };
      },

      statusConverter : function(status, parent) {
        if (!parent.status) {
          return '<I>retrieving...</I>';
        }

        var
              available, unknown, reason,
              remoteStatus = (String(status.remoteStatus)).toLowerCase(),
              sOut = (status.localStatus === 'IN_SERVICE') ? 'In Service' : 'Out of Service';

        available = remoteStatus === 'available';
        unknown = remoteStatus === 'unknown';

        // unavailable: $reason
        reason = remoteStatus.indexOf('unavailable:') === 0 ? '<br/><I>' + Ext.util.Format.htmlEncode(status.remoteStatus.substr(12)) + '</I>' : null;

        if (parent.repoType === 'proxy')
        {
          if (status.proxyMode.search(/BLOCKED/) === 0)
          {
            sOut += status.proxyMode === 'BLOCKED_AUTO' ? ' - Remote Automatically Blocked' : ' - Remote Manually Blocked';
            if (available) {
              sOut += ' and Available';
            } else {
              sOut += ' and Unavailable';
            }
          }
          else
          { // allow
            if (status.localStatus === 'IN_SERVICE')
            {
              if (!available && unknown)
              {
                sOut += unknown ? ' - <I>checking remote...</I>' : ' - Attempting to Proxy and Remote Unavailable';
              }
            }
            else
            { // Out of service
              sOut += available ? ' - Remote Available' : ' - Remote Unavailable';
            }
          }
        }

        if (reason !== null) {
          sOut += reason;
        }

        return sOut;
      },

      updateRepoStatuses : function(status, rec) {
        var i, status2, rec2;
        rec.beginEdit();
        rec.data.status = status;
        rec.set('displayStatus', this.statusConverter(status, rec.data));
        rec.commit();
        rec.endEdit();

        if (status.dependentRepos)
        {
          for (i = 0; i < status.dependentRepos.length; i+=1)
          {
            status2 = status.dependentRepos[i];
            rec2 = rec.store.getById(Sonatype.config.host + Sonatype.config.repos.urls.repositories + '/' + status2.id);
            if (rec2)
            {
              rec2.beginEdit();
              rec2.data.status = status2;
              rec2.set('displayStatus', this.statusConverter(status2, rec2.data));
              rec2.commit();
              rec2.endEdit();
            }
          }
        }

        Sonatype.Events.fireEvent('nexusRepositoryStatus', status);
      },

      restToContentUrl : function(node, repoRecord) {
        return repoRecord.data.resourceURI + '/content' + node.data.relativePath;
      },

      restToRemoteUrl : function(node, repoRecord) {
        return repoRecord.data.remoteUri + node.data.relativePath;
      },

      downloadHandler : function(node, item, event) {
        event.stopEvent();
        window.open(this.restToContentUrl(node, node.attributes.repoRecord));
      },

      downloadFromRemoteHandler : function(node, item, event) {
        event.stopEvent();
        window.open(this.restToRemoteUrl(node, node.attributes.repoRecord));
      },

      deleteRepoItemHandler : function(node) {
        var url = Sonatype.config.repos.urls.repositories + node.id.slice(Sonatype.config.host.length + Sonatype.config.repos.urls.repositories.length);
        // make sure to provide /content path for repository root requests like
        // ../repositories/central
        if (/.*\/repositories\/[^\/]*$/i.test(url))
        {
          url += '/content';
        }
        Sonatype.MessageBox.show({
              animEl : node.getOwnerTree().getEl(),
              title : 'Delete Repository Item?',
              msg : 'Delete the selected "' + node.data.relativePath + '" ' + (node.isLeaf() ? 'file' : 'folder') + '?',
              buttons : Sonatype.MessageBox.YESNO,
              scope : this,
              icon : Sonatype.MessageBox.QUESTION,
              fn : function(btnName) {
                if (btnName === 'yes' || btnName === 'ok')
                {
                  Ext.Ajax.request({
                        url : url,
                        callback : this.deleteRepoItemCallback,
                        success : function(response, options) {
                          options.contentNode.parentNode.removeChild(options.contentNode);
                        },
                        failure : function(response, options) {
                          Sonatype.MessageBox.alert('Error', response.status === 401 ? "You don't have permission to delete artifacts in this repository" : "The server did not delete the file/folder from the repository");
                        },
                        scope : this,
                        contentNode : node,
                        method : 'DELETE'
                      });
                }
              }
            });
      },

      onRepositoryMenuInit : function(menu, repoRecord) {
        var
              repo = repoRecord.data,
              actions = this.repoActions;

        if (repoRecord.id.substring(0, 4) === 'new_' || !repo.userManaged) {
          return;
        }

        actions.clearCache.maybeAddToMenu(menu, repo);
        actions.rebuildMetadata.maybeAddToMenu(menu, repo);

        menu.add('-');

        actions.blockProxy.maybeAddToMenu(menu, repo);
        actions.allowProxy.maybeAddToMenu(menu, repo);
        actions.putOutOfService.maybeAddToMenu(menu, repo);
        actions.putInService.maybeAddToMenu(menu, repo);

        menu.add('-');
      },

      onRepositoryContentMenuInit : function(menu, repoRecord, contentRecord) {
        var
              repo = repoRecord.data,
              actions = this.repoActions;

        contentRecord.data.resourceURI = contentRecord.data.resourceURI || contentRecord.data.id;

        actions.clearCache.maybeAddToMenu(menu, repo, contentRecord);
        actions.rebuildMetadata.maybeAddToMenu(menu, repo, contentRecord);

        if (contentRecord.isLeaf())
        {
          menu.add('-');
          actions.downloadRemoteItem.maybeAddToMenu(menu, repo, contentRecord);
          actions.downloadItem.maybeAddToMenu(menu, repo, contentRecord);
        }

        if (!contentRecord.isRoot && repoRecord.data.repoType !== 'group')
        {
          actions.viewRemote.maybeAddToMenu(menu, repo, contentRecord);
          menu.add('-');
          actions.deleteRepoItem.maybeAddToMenu(menu, repo, contentRecord);
        }
      }
    });

Sonatype.repoServer.DefaultRepoHandler = new Sonatype.repoServer.AbstractRepoPanel();
Sonatype.Events.addListener('repositoryMenuInit', Sonatype.repoServer.DefaultRepoHandler.onRepositoryMenuInit, Sonatype.repoServer.DefaultRepoHandler);
Sonatype.Events.addListener('repositoryContentMenuInit', Sonatype.repoServer.DefaultRepoHandler.onRepositoryContentMenuInit, Sonatype.repoServer.DefaultRepoHandler);

});

