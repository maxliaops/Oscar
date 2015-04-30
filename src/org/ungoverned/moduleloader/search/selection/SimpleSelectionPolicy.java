/*
 * ModuleLoader - A generic, policy-driven class loader.
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.moduleloader.search.selection;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ModuleEvent;
import org.ungoverned.moduleloader.ModuleListener;
import org.ungoverned.moduleloader.search.ImportSearchPolicy;
import org.ungoverned.moduleloader.search.SelectionPolicy;
import org.ungoverned.moduleloader.search.CompatibilityPolicy;

/**
 * This class implements a reasonably simple selection policy for the
 * <tt>ImportSearchPolicy</tt>. When given a choice, this selection
 * policy will always select the newest version of the available
 * candidates to satisfy the import identifier. In the case where
 * a candidate has already been selected for a given import identifier,
 * then the previously selected module will be returned, if possible.
 * If it is not possible to return the previously selected module, then
 * a <tt>null</tt> is returned. This policy assumes that classes are
 * shared globally.
**/
public class SimpleSelectionPolicy implements SelectionPolicy, ModuleListener
{
    private Map m_resolvedPackageMap = new HashMap();
    private Map m_resolvedModuleMap = new HashMap();

    /**
     * Selects a single module to resolve the specified import identifier
     * from the array of compatible candidate modules. If the import
     * identifier has not been resolved before, then this selection policy
     * chooses the module that exports the newest version of the
     * import identifer. If the import identifier has been resolved already,
     * then the same module that was chosen before is chosen again.
     * This ensures that all modules use the same version of all
     * exported classes.
     * @param module the module that is importing the target.
     * @param identifier the identifier of the import target.
     * @param version the version number of the import target.
     * @param candidates array of compatible candidate modules from which to choose.
     * @return the selected module or <tt>null</tt> if no module
     *         can be selected.
    **/
    public synchronized Module select(Module module, Object identifier,
        Object version, Module[] candidates, CompatibilityPolicy compatPolicy)
    {
        // See if package is already resolved.
        Module selModule = (Module) m_resolvedPackageMap.get(identifier);

        // If no module was previously selected to export the package,
        // then try to choose one now.
        if (selModule == null)
        {
            Object selVersion = null;

            // Examine all exported instances of the identifier and
            // choose the one with the newest version number. If
            // there is more than one source for the newest version,
            // then just select the first one found.
            for (int i = 0; i < candidates.length; i++)
            {
                Object tmpVersion =
                    ImportSearchPolicy.getExportVersion(candidates[i], identifier);

                // If this is the first comparison, then
                // just record it.
                if (selVersion == null)
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                }
                // If the current export package version is greater
                // than the selected export package version, then
                // record it instead.
                else if (compatPolicy.compare(identifier, tmpVersion, identifier, selVersion) >= 0)
                {
                    selModule = candidates[i];
                    selVersion = tmpVersion;
                }
            }

            m_resolvedPackageMap.put(identifier, selModule);
            m_resolvedModuleMap.put(selModule, selModule);
        }
        // See if the previously selected export module satisfies
        // the current request, otherwise return null.
        else
        {
            Object selVersion =
                ImportSearchPolicy.getExportVersion(selModule, identifier);
            Module tmpModule = selModule;
            selModule = null;
            if (compatPolicy.isCompatible(identifier, selVersion, identifier, version))
            {
                selModule = tmpModule;
            }
        }

        return selModule;
    }

    public void moduleAdded(ModuleEvent event)
    {
    }

    public void moduleReset(ModuleEvent event)
    {
        moduleRemoved(event);
    }

    public synchronized void moduleRemoved(ModuleEvent event)
    {
        // If the module that was removed was chosen for
        // exporting identifier, then flush it from our
        // data structures; we assume here that the application
        // will flush references to the removed module's classes.
        if (m_resolvedModuleMap.get(event.getModule()) != null)
        {
            // Remove from module map.
            m_resolvedModuleMap.remove(event.getModule());
            // Remove each exported package from package map.
            Iterator iter = m_resolvedPackageMap.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry) iter.next();
                if (entry.getValue() == event.getModule())
                {
                    iter.remove();
                }
            }
        }
    }
}