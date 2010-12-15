package org.alfresco.module.org_alfresco_module_wcmquickstart.util;

import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface AssetSerializer
{

    void start(Writer underlyingWriter) throws AssetSerializationException;

    void end() throws AssetSerializationException;

    void writeHeader(Map<QName, Serializable> properties) throws AssetSerializationException;
    
    void writeNode(NodeRef nodeRef, QName type, Map<QName, Serializable> properties) throws AssetSerializationException;

}