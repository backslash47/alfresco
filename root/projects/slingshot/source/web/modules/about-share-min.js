(function(){var c=YAHOO.util.Dom;Alfresco.module.AboutShare=function(e){var d=Alfresco.util.ComponentManager.get(this.id);if(d!==null){throw new Error("An instance of Alfresco.module.AboutShare already exists.")}Alfresco.module.AboutShare.superclass.constructor.call(this,"Alfresco.module.AboutShare",e,["container","connection","json"]);return this};YAHOO.extend(Alfresco.module.AboutShare,Alfresco.component.Base,{scrollpos:0,show:function a(){if(this.widgets.panel){this.widgets.panel.show()}else{Alfresco.util.Ajax.request({url:Alfresco.constants.URL_SERVICECONTEXT+"modules/about-share",dataObj:{htmlid:this.id},successCallback:{fn:this.onTemplateLoaded,scope:this},execScripts:true,failureMessage:"Could not load About Share template"})}},onTemplateLoaded:function b(d){var h=document.createElement("div");h.innerHTML=d.serverResponse.responseText;var e=c.getFirstChild(h);this.widgets.panel=Alfresco.util.createYUIPanel(e,{draggable:false});if(YAHOO.env.ua.ie===0||YAHOO.env.ua.ie>7){if(YAHOO.env.ua.webkit&&!YAHOO.env.ua.ios){this.widgets.panel.beforeShowEvent.subscribe(function(){c.setStyle(document.body,"-webkit-perspective","800");c.addClass(this.element,"appear")});this.widgets.panel.hide=function(){var i=this;this.element.addEventListener("webkitAnimationEnd",function(){if(c.hasClass(this,"spindrop")){i.cfg.setProperty("visible",false);c.removeClass(this,"appear");c.removeClass(this,"spindrop");c.setStyle(document.body,"-webkit-perspective",null)}},false);c.addClass(this.element,"spindrop")}}c.setStyle(this.id+"-contributions","display","block");var f=this;setInterval(function g(){var j=c.get(f.id+"-contributions");var i=f.scrollpos++;if(i>j.clientHeight){i=f.scrollpos=0}j.style.top="-"+i+"px"},80)}this.widgets.panel.show()}})})();Alfresco.module.getAboutShareInstance=function(){var a="alfresco-AboutShare-instance";return Alfresco.util.ComponentManager.get(a)||new Alfresco.module.AboutShare(a)};