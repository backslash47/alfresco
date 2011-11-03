/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
package org.alfresco.module.vti.web.ws;

import org.alfresco.module.vti.handler.ListServiceHandler;
import org.alfresco.repo.site.SiteDoesNotExistException;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;

/**
 * Class for handling the AddList soap method
 * 
 * @author Nick Burch
 */
public class AddListEndpoint extends AbstractEndpoint
{
	private final static Log logger = LogFactory.getLog(AddListEndpoint.class);

    // handler that provides methods for operating with lists
    private ListServiceHandler handler;

    // xml namespace prefix
    private static String prefix = "listsws";

    /**
     * constructor
     *
     * @param handler
     */
    public AddListEndpoint(ListServiceHandler handler)
    {
        this.handler = handler;
    }

    /**
     * Deletes document workspace
     * 
     * @param soapRequest Vti soap request ({@link VtiSoapRequest})
     * @param soapResponse Vti soap response ({@link VtiSoapResponse}) 
     */
    public void execute(VtiSoapRequest soapRequest, VtiSoapResponse soapResponse) throws Exception   {
       if (logger.isDebugEnabled()) {
          logger.debug("SOAP method with name " + getName() + " is started.");
       }

       // mapping xml namespace to prefix
       SimpleNamespaceContext nc = new SimpleNamespaceContext();
       nc.addNamespace(soapUriPrefix, soapUri);
       nc.addNamespace(prefix, namespace);
       
       String host = getHost(soapRequest);
       String context = soapRequest.getAlfrescoContextName();
       String dws = getDwsFromUri(soapRequest);        

       // Get the listName parameter from the request
       XPath listNameXPath = new Dom4jXPath(buildXPath(prefix, "/AddList/listName"));
       listNameXPath.setNamespaceContext(nc);
       Element listNameE = (Element) listNameXPath.selectSingleNode(soapRequest.getDocument().getRootElement());
       String listName = null;
       if (listNameE != null)
       {
          listName = listNameE.getTextTrim();
       }
       
       // Get the description parameter from the request
       XPath descriptionXPath = new Dom4jXPath(buildXPath(prefix, "/AddList/description"));
       descriptionXPath.setNamespaceContext(nc);
       Element descriptionE = (Element) descriptionXPath.selectSingleNode(soapRequest.getDocument().getRootElement());
       String description = null;
       if (descriptionE != null)
       {
          description = descriptionE.getTextTrim();
       }
       
       // Get the template ID parameter from the request
       XPath templateXPath = new Dom4jXPath(buildXPath(prefix, "/AddList/templateID"));
       templateXPath.setNamespaceContext(nc);
       Element templateE = (Element) templateXPath.selectSingleNode(soapRequest.getDocument().getRootElement());
       int templateID = -1;
       if (templateE != null)
       {
          templateID = Integer.parseInt( templateE.getTextTrim() );
       }
       if(templateID < 0)
       {
          throw new VtiSoapException("Invalid Template ID", -1);
       }

       
       // Have the List Created
       try
       {
          handler.createList(listName, description, dws, templateID);
       }
       catch(SiteDoesNotExistException se)
       {
          throw new VtiSoapException("No site found with name '" + dws + "'", 0x81020012l, se);
       }
       catch(DuplicateChildNodeNameException dcnne)
       {
          throw new VtiSoapException("List name already in use", 0x81020012l, dcnne);
       }
       catch(InvalidTypeException ite)
       {
          throw new VtiSoapException("Template ID not known", 0x8107058al, ite); 
       }
       
       // TODO Return the valid response contents
       Element root = soapResponse.getDocument().addElement("AddListResponse", namespace);
       Element addListResult = root.addElement("AddListResult");

       if (logger.isDebugEnabled()) {
          logger.debug("SOAP method with name " + getName() + " is finished.");
       }        
    }

}