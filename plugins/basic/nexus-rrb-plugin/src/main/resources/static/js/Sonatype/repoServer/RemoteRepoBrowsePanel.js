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
define('Sonatype/repoServer/RemoteRepoBrowsePanel', function() {

  function printallmembers( obj ) {
    var str = '';
    for( var memb in obj )
      str += memb + ' = ' + obj[memb] + '\n';

    return str;
  }

  Sonatype.Events.addListener( 'repositoryViewInit', function( cardPanel, rec ) {
    var sp = Sonatype.lib.Permissions;
    if ( sp.checkPermission( 'nexus:browseremote', sp.READ)
          && (['maven1', 'maven2', 'obr', 'p2'].indexOf(rec.data.format) > -1)
          && rec.data.remoteUri //only add panel if there is a remoteUri
          && rec.data.remoteUri.match( /^(?:http|https|ftp):\/\//i ) ) { //and it is valid uri (to rule out special proxy repo types, such as procurement)
  //	  alert(printallmembers(rec.data));

      cardPanel.add( new Sonatype.repoServer.RemoteRepositoryBrowsePanel( {
        payload: rec,
        name: 'browseremote',
        tabTitle: 'Browse Remote'
      } ) );
    }
  } );

  Sonatype.repoServer.RemoteRepositoryBrowsePanel = function( config ) {
      var config = config || {};
      var defaultConfig = {
        titleColumn: 'name'
      };
      Ext.apply( this, config, defaultConfig );

      this.oldSearchText = '';
      this.searchTask = new Ext.util.DelayedTask( this.startSearch, this, [this]);
      this.nodeContextMenuEvent = 'repositoryContentMenuInit';

      Sonatype.repoServer.RemoteRepositoryBrowsePanel.superclass.constructor.call( this, {
        anchor: '0 -2',
        bodyStyle: 'background-color:#FFFFFF',
        animate: true,
        lines: false,
        autoScroll: true,
        containerScroll: true,
        rootVisible: true,
        enableDD: false,
        tbar: [
          {
            text: 'Refresh',
            icon: Sonatype.config.resourcePath + '/static/images/icons/arrow_refresh.png',
            cls: 'x-btn-text-icon',
            scope: this,
            handler: this.refreshHandler
          },
          ' ',
          'Path Lookup:',
          {
            xtype: 'nexussearchfield',
            searchPanel: this,
            width: 400,
            enableKeyEvents: true,
            listeners: {
              'keyup': {
                fn: function( field, event ) {
                  var key = event.getKey();
                  if ( ! event.isNavKeyPress() ) {
                    this.searchTask.delay( 200 );
                  }
                },
                scope: this
              },
              'render': function(c) {
                Ext.QuickTips.register({
                  target: c.getEl(),
                  text: 'Enter a complete path to lookup, for example org/sonatype/nexus'
                });
              }
            }
          }
        ],
        loader: new Ext.tree.SonatypeTreeLoader( {
          url: '',
          listeners: {
            loadexception: this.treeLoadExceptionHandler,
            scope: this
          }
        } ),
        listeners: {
          click: this.nodeClickHandler,
          //remove existing right-click menu
  //	      contextMenu: this.nodeContextMenuHandler,
          expandnode: this.indexBrowserExpandFollowup,
          scope: this
        }
      } );

      new Ext.tree.TreeSorter( this, { folderSort:true } );

      var root = new Ext.tree.AsyncTreeNode( {
        text: this.payload.data[this.titleColumn],
        id: this.getBrowsePath( this.payload.data.resourceURI,'',this.payload.data.id ),
        singleClickExpand: true,
        expanded: false
      } );

      this.setRootNode( root );
    };

    Ext.extend( Sonatype.repoServer.RemoteRepositoryBrowsePanel, Ext.tree.TreePanel, {

      getBrowsePath: function( baseUrl, remoteUrl, id ) {
      var modUrl=baseUrl+"/remotebrowser/";
      return modUrl;
      },

      getBrowsePathSnippet: function() {
        return this.browseIndex ?
          Sonatype.config.browseIndexPathSnippet : Sonatype.config.browsePathSnippet;
      },

      indexBrowserExpandFollowup: function( node ) {
        if ( this.browseIndex && ! node.attributes.localStorageUpdated && node.firstChild ) {
          node.attributes.localStorageUpdated = true;
          Ext.Ajax.request({
            url: node.id.replace( Sonatype.config.browseIndexPathSnippet, Sonatype.config.browsePathSnippet ) + '?isLocal',
            suppressStatus: 404,
            success: function( response, options ) {
              var decodedResponse = Ext.decode( response.responseText );
              if ( decodedResponse.data ) {
                var data = decodedResponse.data;
                for ( var j = 0; j < node.childNodes.length; j++ ) {
                  var indexNode = node.childNodes[j];
                  indexNode.attributes.localStorageUpdated = true;
                  for ( var i = 0; i < data.length; i++ ) {
                    var contentNode = data[i];
                    if ( contentNode.text == indexNode.text ) {
                      indexNode.ui.iconNode.className = 'x-tree-node-nexus-icon';
                      indexNode.attributes.localStorageUpdated = false;
                      break;
                    }
                  }
                }
              }
            },
            failure: function( response, options ) {
              for ( var j = 0; j < node.childNodes.length; j++ ) {
                node.childNodes[j].attributes.localStorageUpdated = true;
              }
            },
            scope: this
          });
        }
      },

      nodeClickHandler: function( node, e ) {
        if ( e.target.nodeName == 'A' ) return; // no menu on links

        if ( this.nodeClickEvent ) {
          Sonatype.Events.fireEvent( this.nodeClickEvent, node );
        }
      },

      nodeContextMenuHandler: function( node, e ) {
        if ( e.target.nodeName == 'A' ) return; // no menu on links

        if ( !this.payload.data.showCtx ) {
          return;
        }

        if ( this.nodeContextMenuEvent ) {

          node.attributes.repoRecord = this.payload;
          node.data = node.attributes;

          var menu = new Sonatype.menu.Menu( {
            payload: node,
            scope: this,
            items: []
          } );

          Sonatype.Events.fireEvent( this.nodeContextMenuEvent, menu, this.payload, node );

          var item;
          while ( ( item = menu.items.first() ) && ! item.text ) {
            menu.remove( item ); // clean up if the first element is a separator
          }
          while ( ( item = menu.items.last() ) && ! item.text ) {
            menu.remove( item ); // clean up if the last element is a separator
          }
          if ( ! menu.items.first() ) return;

          e.stopEvent();
          menu.showAt( e.getXY() );
        }
      },

      refreshHandler: function( button, e ) {
        this.root.setText( this.payload ? this.payload.get( this.titleColumn ) : '/' );
        this.root.attributes.localStorageUpdated = false;
        this.root.id = this.getBrowsePath( this.payload.data.resourceURI,'',this.payload.data.id );
        this.root.reload();
      },

      startSearch: function( p ) {
        var field = p.searchField;
        var searchText = field.getRawValue();

        var treePanel = p;
        if ( searchText ) {
          field.triggers[0].show();
          var justEdited = p.oldSearchText.length > searchText.length;

          var findMatchingNodes = function( root, textToMatch ) {
            var n = textToMatch.indexOf( '/' );
            var remainder = '';
            if ( n > -1 ) {
              remainder = textToMatch.substring( n + 1 );
              textToMatch = textToMatch.substring( 0, n );
            }

            var matchingNodes = [];
            var found = false;
            for ( var i = 0; i < root.childNodes.length; i++ ) {
              var node = root.childNodes[i];

              var text = node.text;
              if ( text == textToMatch ) {
                node.enable();
                node.ensureVisible();
                node.expand();
                found = true;
                if ( ! node.isLeaf() ) {
                  var autoComplete = false;
                  if ( ! remainder && node.childNodes.length == 1 ) {
                    remainder = node.firstChild.text;
                    autoComplete = true;
                  }
                  if ( remainder ) {
                    var s = findMatchingNodes( node, remainder );
                    if ( autoComplete || ( s && s != remainder ) ) {
                      return textToMatch + '/' + ( s ? s : remainder );
                    }
                  }
                }
              }
              else if ( text.substring( 0, textToMatch.length ) == textToMatch ) {
                matchingNodes[matchingNodes.length] = node;
                node.enable();
                if ( matchingNodes.length == 1 ) {
                  node.ensureVisible();
                }
              }
              else {
                node.disable();
                node.collapse( false, false );
              }
            }

            // if only one non-exact match found, suggest the name
            return ! found && matchingNodes.length == 1 ?
              matchingNodes[0].text + '/' : null;
          };

          var s = findMatchingNodes( treePanel.root, searchText );

          p.oldSearchText = searchText;

          // if auto-complete is suggested, and the user hasn't just started deleting
          // their own typing, try the suggestion
          if ( s && ! justEdited && s != searchText ) {
            field.setRawValue( s );
            p.startSearch( p );
          }

        }
        else {
          p.stopSearch( p );
        }
      },

      stopSearch: function( p ) {
        p.searchField.triggers[0].hide();
        p.oldSearchText = '';

        var treePanel = p;

        var enableAll = function( root ) {
          for ( var i = 0; i < root.childNodes.length; i++ ) {
            var node = root.childNodes[i];
            node.enable();
            node.collapse( false, false );
            enableAll( node );
          }
        };
        enableAll( treePanel.root );
      },

      treeLoadExceptionHandler : function( treeLoader, node, response ) {
        if ( response.status == 503 ) {
          if ( Sonatype.MessageBox.isVisible() ) {
            Sonatype.MessageBox.hide();
          }
          node.setText( node.text + ' (Out of Service)' );
        }
        else if ( response.status == 404 || response.status == 400 ) {
          if ( Sonatype.MessageBox.isVisible() ) {
            Sonatype.MessageBox.hide();
          }
          node.setText( node.text + ( node.isRoot ? ' (Not Available)' : ' (Not Found)' ) );
        }
        else if ( response.status == 401 || response.status == 403 ) {
          if ( Sonatype.MessageBox.isVisible() ) {
            Sonatype.MessageBox.hide();
          }
          node.setText( node.text + ' (Access Denied)' );
        }
      }

    } );
});
