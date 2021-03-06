/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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
package org.alfresco.rest.workflow.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.workflow.WorkflowQNameConverter;
import org.alfresco.repo.workflow.activiti.ActivitiConstants;
import org.alfresco.rest.framework.core.exceptions.ApiException;
import org.alfresco.rest.workflow.api.model.TaskVariable;
import org.alfresco.rest.workflow.api.model.Variable;
import org.alfresco.rest.workflow.api.model.VariableScope;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

/**
 * Helper class for handling conversion between variable representations in rest and raw values
 * used in the Activiti-engine.
 *
 * @author Frederik Heremans
 */
public class RestVariableHelper
{
    private NodeService nodeService;
    
    private NamespaceService namespaceService;
    
    private WorkflowQNameConverter qnameConverter;
    
    public static final Set<String> INTERNAL_PROPERTIES = new HashSet<String>(Arrays.asList(ActivitiConstants.VAR_TENANT_DOMAIN));
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
        this.qnameConverter = new WorkflowQNameConverter(namespaceService);
    }
    
    /**
     * @param localVariables raw local task variables, can be null.
     * @param globalvariables raw global taks variables, can be null.
     * @param typeDefinition the typê definition for this task, used to extract types.
     * @return list of {@link TaskVariable}, representing the given raw variables
     */
    public List<TaskVariable> getTaskVariables(Map<String, Object> localVariables, Map<String, Object> globalVariables, TypeDefinition typeDefinition)
    {
        List<TaskVariable> result = new ArrayList<TaskVariable>();
        TypeDefinitionContext context = new TypeDefinitionContext(typeDefinition);
        
        if (localVariables != null) 
        {
            addTaskVariables(result, localVariables, context, VariableScope.LOCAL);
        }
        
        if (globalVariables != null) 
        {
            addTaskVariables(result, globalVariables, context, VariableScope.GLOBAL);
        }
        
        return result;
    }
    
    /**
     * @param variables raw variables
     * @param typeDefinition the typê definition for the start-task of the process, used to extract types.
     * @return list of {@link Variable}, representing the given raw variables
     */
    public List<Variable> getVariables(Map<String, Object> variables, TypeDefinition typeDefinition)
    {
        List<Variable> result = new ArrayList<Variable>();
        TypeDefinitionContext context = new TypeDefinitionContext(typeDefinition);
        
        Variable variable = null;
        for(Entry<String, Object> entry : variables.entrySet()) 
        {
            if(!INTERNAL_PROPERTIES.contains(entry.getKey()))
            {
                variable = new Variable();
                variable.setName(entry.getKey());
                
                // Set value and type
                setVariableValueAndType(variable, entry.getValue(), context);
                result.add(variable);
            }
        }
        return result;
    }
    
    

    /**
     * Converts the raw variables to {@link TaskVariable}s and adds them to the given result-list.
     */
    protected void addTaskVariables(List<TaskVariable> result, Map<String, Object> variables,
                TypeDefinitionContext context, VariableScope scope)
    {
        TaskVariable variable = null;
        for(Entry<String, Object> entry : variables.entrySet()) 
        {
            if(!INTERNAL_PROPERTIES.contains(entry.getKey()))
            {
                variable = new TaskVariable();
                variable.setName(entry.getKey());
                variable.setVariableScope(scope);
                
                // Set value and type
                setVariableValueAndType(variable, entry.getValue(), context);
                result.add(variable);
            }
        }
    }

    /**
     * Sets the variable value with possible conversion to the correct format to be used in the response and sets
     * the type accordingly. If the variables is defined on the {@link TypeDefinition}, the data-type is used. If it's not
     * defined, the type is deducted from the raw variable value.
     */
    protected void setVariableValueAndType(Variable variable, Object value, TypeDefinitionContext context)
    {
        PropertyDefinition propDef = context.getPropertyDefinition(variable.getName());
        if(propDef != null)
        {
            variable.setValue(getSafePropertyValue(value));
            variable.setType(propDef.getDataType().getName().toPrefixString(namespaceService));
        }
        else
        {
            // Not defined as a property, check if it's an association
            AssociationDefinition assocDef = context.getAssociationDefinition(variable.getName());
            if(assocDef != null)
            {
                // Type of variable is the target class-name
                variable.setType(assocDef.getTargetClass().getName().toPrefixString(namespaceService));
                variable.setValue(getAssociationRepresentation(value, assocDef));
            }
            else
            {
                // Variable is not a declared property not association on the type-definition. Revert to using the actual raw value 
                // as base for conversion.
                variable.setValue(getSafePropertyValue(value));
                variable.setType(extractTypeStringFromValue(value));
            }
        }
    }
    
    /**
     * @return object that represents the association value. 
     */
    protected Object getAssociationRepresentation(Object value, AssociationDefinition assocDef)
    {
        Object result = null;
        if(value != null)
        {
            if(assocDef.isTargetMany()) 
            {
                // Construct list of representations of the nodeRefs
                List<Object> list = new ArrayList<Object>();
                if(value instanceof Collection<?>)
                {
                    for(Object entry : (Collection<?>) value)
                    {
                        list.add(getRepresentationForNodeRef(entry, assocDef.getTargetClass()));
                    }
                }
                else
                {
                    // Many-property but only single value present
                    list.add(getRepresentationForNodeRef(value, assocDef.getTargetClass()));
                }
                result = list;
            }
            else
            {
                // Association is a single nodeRef, get representation for it
                result = getRepresentationForNodeRef(value, assocDef.getTargetClass());
            }
        }
        return result;
    }
    
    protected Object getSafePropertyValue(Object value)
    {
        if(value instanceof NodeRef)
        {
            return value.toString();
        }
        else if(value instanceof ScriptNode)
        {
            return ((ScriptNode)value).getNodeRef().toString();
        }
        else if(value instanceof QName) 
        {
            return ((QName) value).toPrefixString(namespaceService);
        }
        
        return value;
    }
    
    protected String getRepresentationForNodeRef(Object value, ClassDefinition classDefinition) 
    {
        // First, extract the referenced node
        NodeRef ref = null;
        if(value instanceof NodeRef) 
        {
            ref = (NodeRef) value;
        }
        else if(value instanceof ScriptNode)
        {
            ref = ((ScriptNode) value).getNodeRef();
        }
        
        if(ref != null)
        {
            if(ContentModel.TYPE_PERSON.equals(classDefinition.getName()))
            {
                // Extract username from person and return
                return (String) nodeService.getProperty(ref, ContentModel.PROP_USERNAME);
            }
            else
            {
                return ref.toString();
            }
        }
        else
        {
            throw new ApiException("Association value did not contian valid nodeRef: " + value);
        }
    }
    
    public QName extractTypeFromValue(Object value) {
        QName type = null;
        if(value instanceof Collection<?>)
        {
            Collection<?> collection = (Collection<?>) value;
            if(collection.size() > 0)
            {
                type = extractTypeFromValue(collection.iterator().next());
            }
        }
        else
        {
            if(value instanceof String) 
            {
                type = DataTypeDefinition.TEXT;
            }
            else if(value instanceof Integer)
            {
                type = DataTypeDefinition.INT;
            }
            else if(value instanceof Long)
            {
                type = DataTypeDefinition.LONG;
            }
            else if(value instanceof Double)
            {
                type = DataTypeDefinition.DOUBLE;
            }
            else if(value instanceof Float)
            {
                type = DataTypeDefinition.FLOAT;
            }
            else if(value instanceof Date)
            {
                type = DataTypeDefinition.DATETIME;
            }
            else if(value instanceof Boolean)
            {
                type = DataTypeDefinition.BOOLEAN;
            }
            else if(value instanceof QName)
            {
                type = DataTypeDefinition.QNAME;
            }
            else if(value instanceof NodeRef || value instanceof ScriptNode)
            {
                type = DataTypeDefinition.NODE_REF;
            }
        }
       
        if(type == null)
        {
            // Type cannot be determined, revert to default for unknown types
            type = DataTypeDefinition.ANY;
        }
        return type;
    }
    
    public String extractTypeStringFromValue(Object value)
    {
       QName type = extractTypeFromValue(value);
       return type.toPrefixString(namespaceService);
    }

    /**
     * Helper contxt class used when checking variable types based on {@link TypeDefinition}. 
     *
     * @author Frederik Heremans
     */
    private class TypeDefinitionContext {
        private Map<String, PropertyDefinition> propertyDefinitions;
        private Map<String, AssociationDefinition> associationDefinitions;

        public TypeDefinitionContext(TypeDefinition typeDefinition)
        {
            propertyDefinitions = new HashMap<String, PropertyDefinition>();
            associationDefinitions = new HashMap<String, AssociationDefinition>();
            
            for (Entry<QName, PropertyDefinition> entry : typeDefinition.getProperties().entrySet())
            {
                propertyDefinitions.put(qnameConverter.mapQNameToName(entry.getKey()), entry.getValue());
            }
            
            for (Entry<QName, AssociationDefinition> entry : typeDefinition.getAssociations().entrySet())
            {
                associationDefinitions.put(qnameConverter.mapQNameToName(entry.getKey()), entry.getValue());
            }
        }
        
        public PropertyDefinition getPropertyDefinition(String rawVariableName) 
        {
            return propertyDefinitions.get(rawVariableName);
        }
        
        public AssociationDefinition getAssociationDefinition(String rawVariableName)
        {
            return associationDefinitions.get(rawVariableName);
        }
    }
}
