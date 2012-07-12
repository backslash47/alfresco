<@markup id="css" >
   <#-- No CSS Dependencies -->
   <@link href="" group=""/>
</@>

<@markup id="js">
   <#-- JavaScript Dependencies -->
   <@script type="text/javascript" src="${url.context}/res/components/dashlets/mini-calendar.js" group="dashlets"/>
</@>

<@markup id="widgets">
   <@createWidgets group="dashlets"/>
</@>

<@markup id="html">
   <@uniqueIdDiv>
      <div class="dashlet calendar">
         <div class="title">${msg("label.header")}</div>
         <div id="${args.htmlid}-eventsContainer" class="body scrollableList" <#if args.height??>style="height: ${args.height}px;"</#if>>
         </div>
      </div>
   </@>
</@>