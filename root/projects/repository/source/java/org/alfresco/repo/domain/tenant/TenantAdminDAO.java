/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
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
package org.alfresco.repo.domain.tenant;

import java.util.List;


/**
 * Data abstraction layer for Tenant entities.
 * 
 * @author janv
 * @since 4.0 (thor)
 */
public interface TenantAdminDAO
{
    /**
     * Create tenant - note: tenant domain must be unique
     * 
     * @param tenantEntity
     * @return
     */
    TenantEntity createTenant(TenantEntity tenantEntity);
    
    /**
     * Get tenant
     * 
     * @param tenantEntity
     * @return
     */
    TenantEntity getTenant(String tenantDomain);
    
    /**
     * List tenants
     * 
     * TODO add filter(s)
     * 
     * @param tenantEntity
     * @return
     */
    List<TenantEntity> listTenants();
    
    /**
     * Get tenant for update
     * 
     * @param tenantEntity
     * @return
     */
    TenantUpdateEntity getTenantForUpdate(String tenantDomain);
    
    /**
     * Update tenant
     * 
     * Note: tenant domain cannot be changed
     * 
     * @param tenantUpdateEntity
     */
    void updateTenant(TenantUpdateEntity tenantUpdateEntity);
    
    /**
     * Delete tenant
     * 
     * @param tenantEntity
     */
    void deleteTenant(String tenantDomain);
}
