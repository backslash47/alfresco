/**
 * Copyright (C) 2005-2013 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * 
 * @module alfresco/prototyping/Preview
 * @extends dijit/_WidgetBase
 * @mixes dijit/_TemplatedMixin
 * @mixes module:alfresco/core/Core
 * @author Dave Draper
 */
define(["dojo/_base/declare",
        "dijit/_WidgetBase", 
        "dijit/_TemplatedMixin",
        "dojo/text!./templates/Preview.html",
        "alfresco/core/Core",
        "alfresco/core/CoreXhr",
        "dojo/dom-construct",
        "dojo/_base/lang",
        "dojo/json",
        "dojo/query",
        "dojo/NodeList-manipulate"], 
        function(declare, _Widget, _Templated, template, AlfCore, CoreXhr, domConstruct, lang, dojoJson, query) {
   
   return declare([_Widget, _Templated, AlfCore, CoreXhr], {
      
      /**
       * An array of the CSS files to use with this widget.
       * 
       * @instance
       * @type cssRequirements {Array}
       */
      cssRequirements: [{cssFile:"./css/Preview.css"}],
      
      /**
       * An array of the i18n files to use with this widget.
       * 
       * @instance
       * @type i18nRequirements {Array}
       */
      i18nRequirements: [{i18nFile: "./i18n/Preview.properties"}],

      /**
       * The HTML template to use for the widget.
       * @instance
       * @type template {String}
       */
      templateString: template,
      
      /**
       * @instance
       */
      postCreate: function alfresco_prototyping_Preview_postCreate() {
         this.alfSubscribe("ALF_GENERATE_PAGE_PREVIEW", lang.hitch(this, "generatePreview"));
      },
      
      /**
       * @instance
       * @param {object} payload An object containing the details of the page definition to preview.
       */
      generatePreview: function alfresco_prototyping_Preview__generatePreview(payload) {

         if (payload != null && payload.pageDefinition != null)
         {
            // Clear out any previous preview...
            domConstruct.empty(this.previewNode);
            
            try
            {
               var pageDefObject = dojoJson.parse(payload.pageDefinition);
               var data = {
                  jsonContent: pageDefObject,
                  widgets: payload.pageDefinition
               };
               this.serviceXhr({
                  url : Alfresco.constants.URL_SERVICECONTEXT + "surf/dojo/xhr/dependencies",
                  data: data,
                  method: "POST",
                  successCallback: this.updatePage,
                  failureCallback: this.onDependencyFailure,
                  callbackScope: this
               });
            }
            catch(e)
            {
               this.alfLog("error", "An error occurred parsing the JSON", e, this);
            }
         }
         else
         {
            this.alfLog("warn", "A request was made to preview a page definition, but no 'pageDefinition' was provided", payload, this);
         }
      },
      
      /**
       * @instance
       */
      updatePage: function alfresco_prototyping_Preview__updatePage(response, originalRequestConfig) {
         // Iterate over the CSS map and append a new <link> element into the <head> element to ensure that all the
         // widgets CSS dependencies are loaded... 
         for (var media in response.cssMap)
         {
            // TODO: query for the node outside of the loop
            // TODO: keep a reference to each node appended and then remove it when the preview is regenerated
            query("head").append('<link rel="stylesheet" type="text/css" href="' + appContext + response.cssMap[media] + '" media="' + media + '">');
         }
         
         // Build in the i18n properties into the global object...
         for (var scope in response.i18nMap)
         {
            if (typeof window[response.i18nGlobalObject].messages.scope[scope] == "undefined")
            {
               // If the scope hasn't already been used then we can just assign it directly...
               window[response.i18nGlobalObject].messages.scope[scope] = response.i18nMap[scope];
            }
            else
            {
               // ...but if the scope already exists, then we need to mixin the new properties...
               lang.mixin(window[response.i18nGlobalObject].messages.scope[scope], response.i18nMap[scope]);
            }
         }
         
         // The data response will contain a MD5 referencing JavaScript resource that we should request that Dojo loads...
         var requires = [Alfresco.constants.URL_RESCONTEXT + response.javaScript];
         require(requires, lang.hitch(this, "processWidgets", originalRequestConfig.data.jsonContent.widgets, this.previewNode));
         
      },

      /**
       * @instance
       */
      onDependencyFailure: function alfresco_prototyping_Preview__onDependencyFailure(response, originalRequestConfig) {
         this.alfLog("error", "An error occurred requesting the XHR dependencies", response, originalRequestConfig);
      }
   });
});