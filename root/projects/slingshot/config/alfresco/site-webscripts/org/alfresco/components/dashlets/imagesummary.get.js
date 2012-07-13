function main()
{
   
   var imageSummary = {
      id : "ImageSummary",
      name : "Alfresco.dashlet.ImageSummary",
      options : {
         siteId : (page.url.templateArgs.site != null) ? page.url.templateArgs.site : ""
      }
   };

   var dashletResizer = {
      id : "DashletResizer",
      name : "Alfresco.widget.DashletResizer",
      initArgs : ["\"" + args.htmlid + "\"", "\"" + instance.object.id + "\""],
      useMessages: false
   };
   
   var dashletTitleBarActions = {
      id : "DashletTitleBarActions",
      name : "Alfresco.widget.DashletTitleBarActions",
      useMessages : false,
      options : {
         actions : [
            {
               cssClass: "help",
               bubbleOnClick:
               {
                  message: msg.get("dashlet.help")
               },
               tooltip: msg.get("dashlet.help.tooltip")
           }
        ]
      }
   };
   model.widgets = [imageSummary, dashletResizer, dashletTitleBarActions];
}

main();
