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
package org.alfresco.jcr.item;

import java.util.List;

import javax.jcr.PathNotFoundException;

import org.alfresco.jcr.session.SessionImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchService;


/**
 * Responsible for finding JCR Items (Nodes, Properties) from Alfresco equivalents 
 * 
 * @author David Caruana
 *
 */
public class ItemResolver
{

    /**
     * Create an Item from a JCR Path
     * 
     * @param context  session context
     * @param from  starting node for path
     * @param path  the path
     * @return  the Item (Node or Property)
     * @throws PathNotFoundException
     */
    public static ItemImpl findItem(SessionImpl context, NodeRef from, String path)
        throws PathNotFoundException
    {
        ItemImpl item = null;
        
        NodeRef nodeRef = getNodeRef(context, from, path);
        if (nodeRef != null)
        {
            item = new NodeImpl(context, nodeRef);
        }
        else
        {
            // TODO: create property
        }
        
        if (item == null)
        {
            throw new PathNotFoundException("Path " + path + " not found.");
        }

        return item;
    }

    /**
     * Create an Node from a JCR Path
     * 
     * @param context  session context
     * @param from  starting node for path
     * @param path  the path
     * @return  the Item (Node or Property)
     * @throws PathNotFoundException
     */
    public static NodeImpl findNode(SessionImpl context, NodeRef from, String path)
        throws PathNotFoundException
    {
        NodeRef nodeRef = getNodeRef(context, from, path);
        if (nodeRef == null)
        {
            throw new PathNotFoundException("A node does not exist at path " + path + " relative to node " + from);
        }
        return new NodeImpl(context, nodeRef);
    }
    
    /**
     * Determine if Item exists
     * 
     * @param context  session context
     * @param from  starting node for path
     * @param path  the path
     * @return  true => exists, false => no it doesn't
     */
    public static boolean itemExists(SessionImpl context, NodeRef from, String path)
    {
        boolean exists = nodeExists(context, from, path);
        if (!exists)
        {
            // TODO: Check for property
        }
        return exists;
    }

    /**
     * Determine if Node exists
     * 
     * @param context  session context
     * @param from  starting node for path
     * @param path  the path
     * @return  true => exists, false => no it doesn't
     */
    public static boolean nodeExists(SessionImpl context, NodeRef from, String path)
    {
        NodeRef nodeRef = getNodeRef(context, from, path);
        return nodeRef != null;
    }
    
    /**
     * Gets the Node Reference for the node at the specified path
     * 
     * @param context  session context
     * @param from  the starting node for the path
     * @param path  the path
     * @return  the node reference (or null if not found)
     */
    public static NodeRef getNodeRef(SessionImpl context, NodeRef from, String path)
    {
        NodeRef nodeRef = null;
        
        // TODO: Support JCR Path
        // TODO: Catch malformed path and return false (per Specification)
        SearchService search = context.getRepositoryImpl().getServiceRegistry().getSearchService(); 
        List<NodeRef> nodeRefs = search.selectNodes(from, path, null, context.getNamespaceResolver(), false);
        if (nodeRefs != null && nodeRefs.size() > 0)
        {
            nodeRef = nodeRefs.get(0);
        }
            
        return nodeRef;
    }
    
}
