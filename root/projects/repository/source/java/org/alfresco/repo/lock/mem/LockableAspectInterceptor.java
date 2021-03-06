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
package org.alfresco.repo.lock.mem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * NodeService interceptor to spoof the cm:lockable aspect when reading a node that has
 * an ephemeral lock on it. LockService policies such as beforeDeleteNode that protect
 * locked nodes, would not fire for nodes with ephemeral locks on them unless
 * they are reported to have the cm:lockable aspect on them. As ephemeral locks are only held in memory
 * the nodes have not been marked with this aspect, so the aspect must be spoofed.
 * <p>
 * There is no need for this interceptor to worry about setProperty, removeProperty, setProperties,
 * addProperties since if a service other than the lock service attempts to set cm:lockable properties when it
 * is already locked, the beforeUpdateNode node service policy will disallow the update anyway.
 *  
 * @author Matt Ward
 */
public class LockableAspectInterceptor implements MethodInterceptor
{
    private LockStore lockStore;
    private AuthenticationService authenticationService;
    private NodeService nodeService;
    private final ThreadLocal<Boolean> threadEnabled;

    /**
     * Default constructor.
     */
    public LockableAspectInterceptor()
    {
        threadEnabled = new ThreadLocal<Boolean>()
        {
            @Override
            protected Boolean initialValue()
            {
                // The interceptor is enabled for all threads by default.
                return Boolean.TRUE;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable
    {
        if (!threadEnabled.get())
        {
            // Interceptor is not enabled for this thread.
            return invocation.proceed();
        }
        
        final String methodName = invocation.getMethod().getName();
        final Object[] args = invocation.getArguments();
        
        if (methodName.equals("hasAspect"))
        {
            NodeRef nodeRef = (NodeRef) args[0];
            QName aspectTypeQName = (QName) args[1];
            
            // If the hasAspect() call is checking for cm:lockable and this is an ephemeral lock,
            // then spoof the aspect's existence on the node.
            if (ContentModel.ASPECT_LOCKABLE.equals(aspectTypeQName) && hasEphemeralLock(nodeRef))
            {
                return true;
            }
            return invocation.proceed();
        }
        else if (methodName.equals("getAspects"))
        {
            NodeRef nodeRef = (NodeRef) args[0];
            Set<QName> aspects = (Set<QName>) invocation.proceed();
            if (hasEphemeralLock(nodeRef) && !aspects.contains(ContentModel.ASPECT_LOCKABLE))
            {
                aspects.add(ContentModel.ASPECT_LOCKABLE);
            }
            return aspects;
        }
        else if (methodName.equals("getProperties"))
        {
            NodeRef nodeRef = (NodeRef) args[0];
            lockStore.acquireConcurrencyLock(nodeRef);
            try
            {
                Map<QName, Serializable> properties = (Map<QName, Serializable>) invocation.proceed();
                if (hasEphemeralLock(nodeRef))
                {
                    LockState lockState = lockStore.get(nodeRef);
                    String userName = lockState.getOwner();
                    properties.put(ContentModel.PROP_LOCK_OWNER, userName);
                    properties.put(ContentModel.PROP_LOCK_TYPE, lockState.getLockType().toString());
                    properties.put(ContentModel.PROP_EXPIRY_DATE, lockState.getExpires());
                }
                return properties;
            }
            finally
            {
                lockStore.releaseConcurrencyLock(nodeRef);
            }
        }
        else if (methodName.equals("getProperty"))
        {
            NodeRef nodeRef = (NodeRef) args[0];
            QName propQName = (QName) args[1];
            
            // Avoid locking unless it is an interesting property.
            if (isLockProperty(propQName))
            {
                lockStore.acquireConcurrencyLock(nodeRef);
                try
                {
                    if (hasEphemeralLock(nodeRef))
                    {
                        LockState lockState = lockStore.get(nodeRef);
                        if (ContentModel.PROP_LOCK_OWNER.equals(propQName))
                        {
                            return lockState.getOwner();
                        }
                        else if (ContentModel.PROP_LOCK_TYPE.equals(propQName))
                        {
                            return lockState.getLockType().toString();
                        }
                        else if (ContentModel.PROP_EXPIRY_DATE.equals(propQName))
                        {
                            return lockState.getExpires();
                        }
                    }
                }
                finally
                {
                    lockStore.releaseConcurrencyLock(nodeRef);
                }
            }
            return invocation.proceed();
        }
        else if (methodName.equals("setProperties"))
        {
            // If a client has retrieved the node's properties using getProperties and is saving them
            // back using setProperties then it is important that lock properties (e.g. cm:lockType, cm:lockOwner)
            // are not persisted if there is an ephemeral lock present - otherwise the ephemeral lock will
            // be effectively converted into a peristent lock.
            NodeRef nodeRef = (NodeRef) args[0];
            Map<QName, Serializable> newProperties = (Map<QName, Serializable>) args[1];

            lockStore.acquireConcurrencyLock(nodeRef);
            try
            {
                if (hasEphemeralLock(nodeRef) && containsLockProperty(newProperties))
                {
                    Map<QName, Serializable> convertedProperties = filterLockProperties(newProperties);
                    // Now complete the call by passing the converted properties
                    nodeService.setProperties(nodeRef, convertedProperties);
                    return null;
                }
                else
                {
                    return invocation.proceed();
                }
            }
            finally
            {
                lockStore.releaseConcurrencyLock(nodeRef);
            }
        }
        else
        {
            // If not a special case, invoke the original method.
            return invocation.proceed();
        }
    }

    /**
     * Enables the interceptor for the current thread. This would normally be used in a finally
     * block to re-enable the interceptor for a previously {@link #disableForThread() disabled} thread.
     */
    public void enableForThread()
    {
        threadEnabled.set(Boolean.TRUE);
    }
    
    /**
     * Disables the interceptor for the current thread. Follow with a try/finally block as described
     * for {@link #enableForThread()}.
     */
    public void disableForThread()
    {
        threadEnabled.set(Boolean.FALSE);
    }
    
    /**
     * Given a set of properties, removes those where <code>isLockProperty(propQName) == true</code>.
     * 
     * @param properties
     * @return filtered properties.
     */
    private Map<QName, Serializable> filterLockProperties(Map<QName, Serializable> properties)
    {
        Map<QName, Serializable> filteredProps = new HashMap<QName, Serializable>(properties.size() * 2);
        
        // Only add non-lock properties
        for (QName propQName : properties.keySet())
        {
            if (!isLockProperty(propQName))
            {
                filteredProps.put(propQName, properties.get(propQName));
            }
        }
        return filteredProps;
    }
    
    /**
     * Does the collection contain a lock related property?
     */
    private boolean containsLockProperty(Map<QName, ?> properties)
    {
        boolean containsLockProperty = (
                    properties.containsKey(ContentModel.PROP_LOCK_OWNER) ||
                    properties.containsKey(ContentModel.PROP_LOCK_TYPE) ||
                    properties.containsKey(ContentModel.PROP_EXPIRY_DATE)                    
        );
        return containsLockProperty;
    }
    
    /**
     * Return true if the specified property QName is for a lock-related property.
     */
    private boolean isLockProperty(QName propQName)
    {
        boolean isLockProp =
                    propQName.equals(ContentModel.PROP_LOCK_OWNER) ||
                    propQName.equals(ContentModel.PROP_LOCK_TYPE) ||
                    propQName.equals(ContentModel.PROP_EXPIRY_DATE);
        return isLockProp;
    }

    private boolean hasEphemeralLock(NodeRef nodeRef)
    {
        LockState lockState = lockStore.get(nodeRef);
        boolean ephemeral = lockState != null &&
                            lockState.isLockInfo() &&
                            lockState.getLifetime() == Lifetime.EPHEMERAL;
        return ephemeral;
    }

    public void setLockStore(LockStore lockStore)
    {
        this.lockStore = lockStore;
    }

    public void setAuthenticationService(AuthenticationService authenticationService)
    {
        this.authenticationService = authenticationService;
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
}
