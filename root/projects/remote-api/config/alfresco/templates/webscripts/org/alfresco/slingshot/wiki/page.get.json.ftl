{
<#if result.page??>
   <#assign page = result.page>
   <#assign node = result.node>
   "name": "${page.systemName}",
   "title": "<#if page.title?has_content>${page.title}<#else>${page.systemName?replace("_", " ")}</#if>",
   "pagetext": "${page.contents}",
   "tags": [
   <#list result.tags as tag>
      "${tag}"<#if tag_has_next>,</#if>
   </#list>
   ],
   "links": [
   <#list result.links as link>
      "${link}"<#if link_has_next>,</#if>
   </#list>
   ],
   "pageList": [
   <#list result.pageList as p>
      "${p}"<#if p_has_next>,</#if>
   </#list>
   ],
   <#if node.hasAspect("cm:versionable")>
   "versionhistory": [
      <#list node.versionHistory as record>
   {
      "name": "${record.name}",
      "version": "${record.versionLabel}",
      "versionId": "${record.id}",
      "date": "${record.createdDate?datetime?string("yyyy-mm-dd'T'HH:MM:ss")}",
      "author": "${record.creator}"     
   }<#if record_has_next>,</#if>
      </#list> 
   ],
   </#if>  
   "permissions":
   {
      "create": ${result.container.hasPermission("CreateChildren")?string},
      "edit": ${node.hasPermission("Write")?string},
      "delete": ${node.hasPermission("Delete")?string}
   }
<#else>
   "error" : "${result.error!""}"
</#if>
}
