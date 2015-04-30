/*
 * Oscar - An implementation of the OSGi framework.
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
package org.ungoverned.oscar;

import java.net.URL;

import org.ungoverned.moduleloader.Module;
import org.ungoverned.moduleloader.ResourceNotFoundException;
import org.ungoverned.moduleloader.search.ImportSearchPolicy;

/**
 * This class extends <tt>ImportSearchPolicy</tt> in order to implement
 * dynamic package import as defined by the OSGi specification. It does this
 * by overriding the <tt>ImportSearchPolicy.findClass()</tt> and
 * <tt>ImportSearchPolicy.findResource()</tt> methods. By default, this
 * class lets requests fall through to the super class, but if either
 * method throws an exception or returns <tt>null</tt>, then it checks
 * the dynamic import meta-data for the associated bundle.
**/
public class OSGiImportSearchPolicy extends ImportSearchPolicy
{
    /**
     * This is the name of the "dynamic-imports" meta-data attribute that
     * should be attached to each module. The value of this attribute
     * is of type <tt>String[]</tt> and contains dynamic import package
     * specs as defined by the OSGi bundle manifest specification.
    **/
    public static final String DYNAMIC_IMPORTS_ATTR = "dynamic-imports";

    private Oscar m_oscar = null;

    public OSGiImportSearchPolicy(Oscar oscar)
    {
        super(new OSGiCompatibilityPolicy(oscar), new OSGiSelectionPolicy(oscar));
        m_oscar = oscar;
    }

    public Class findClass(Module module, String name)
        throws ClassNotFoundException
    {
        Class clazz = super.findClass(module, name);

        if (clazz == null)
        {
            clazz = findClassDynamic(module, name);
        }

        return clazz;
    }

    public URL findResource(Module module, String name)
        throws ResourceNotFoundException
    {
        URL url = super.findResource(module, name);

        if (url == null)
        {
            url = findResourceDynamic(module, name);
        }

        return url;
    }

    protected Class findClassDynamic(Module module, String name)
    {
        // There is an overriding assumption here that a package is
        // never split across bundles. If a package can be split
        // across bundles, then this will fail.

        try
        {
            BundleImpl bundle =
                (BundleImpl) m_oscar.getBundle(
                    BundleInfo.getBundleIdFromModuleId(module.getId()));
            BundleInfo info = bundle.getInfo();

            // See if the target package name matches the package
            // spec for this dynamic import.
            int idx = name.lastIndexOf('.');
            if (idx < 0)
            {
                // Ignore classes with no package.
                return null;
            }
            String pkgTarget = name.substring(0, idx);

            // Check the dynamic import specs for a match of
            // the target package.
            String[] dynImports = OSGiImportSearchPolicy.getDynamicImports(module);
            boolean matches = false;
            for (int i = 0; !matches && (i < dynImports.length); i++)
            {
                // Star matches everything.
                if (dynImports[i].equals("*"))
                {
                    matches = true;
                }
                // Packages ending in ".*" must match starting strings.
                else if (dynImports[i].endsWith(".*"))
                {
                    matches = pkgTarget.regionMatches(
                        0, dynImports[i], 0, dynImports[i].length() - 2);
                }
                // Or we can have a precise match.
                else
                {
                    matches = pkgTarget.equals(dynImports[i]);
                }
            }

            // If the target package does not match any dynamically imported
            // packages or if the module already imports the target package,
            // then just return null. The module may already import the target
            // package if the class being searched for does not actually exist.
            if (!matches || ImportSearchPolicy.doesImport(module, pkgTarget))
            {
                return null;
            }

            // Try to add the import, which will also resolve the import
            // if the module is currently active.
            int[] version = { 0, 0, 0 };
            if (m_oscar.addImport(module, pkgTarget, version, false))
            {
                // Get the module that resolves the package so we can use
                // its class loader to load the target class; it is only
                // necessary to load the class manually this first time
                // because we have dynamically add the dynamically imported
                // package to the importing module's meta-data, so future
                // attempts to load classes from the dynamically imported
                // package will be processed in the normal fashion.
                Module resolvingModule =
                    ImportSearchPolicy.getImportResolvingModule(module, pkgTarget);
                if (resolvingModule != null)
                {
                    // Now try to get the class from the exporter.
                    return resolvingModule.getClassLoader().loadClass(name);
                }
            }
        }
        catch (Exception ex)
        {
            Oscar.error("Unable to dynamically import package.", ex);
        }

        return null;
    }

    protected URL findResourceDynamic(Module module, String name)
    {
        // There is an overriding assumption here that a package is
        // never split across bundles. If a package can be split
        // across bundles, then this will fail.

        try
        {
            BundleImpl bundle =
                (BundleImpl) m_oscar.getBundle(
                    BundleInfo.getBundleIdFromModuleId(module.getId()));
            BundleInfo info = bundle.getInfo();

            // See if the target package name matches the package
            // spec for this dynamic import.
            int idx = name.lastIndexOf('/');
            if (idx < 0)
            {
                // Ignore resources with no package.
                return null;
            }
            String pkgTarget = name.substring(0, idx);
            pkgTarget = pkgTarget.replace('/', '.');

            // Check the dynamic import specs for a match of
            // the target package.
            String[] dynImports = OSGiImportSearchPolicy.getDynamicImports(module);
            boolean matches = false;
            for (int i = 0; !matches && (i < dynImports.length); i++)
            {
                // Star matches everything.
                if (dynImports[i].equals("*"))
                {
                    matches = true;
                }
                // Packages ending in ".*" must match starting strings.
                else if (dynImports[i].endsWith(".*"))
                {
                    matches = pkgTarget.regionMatches(
                        0, dynImports[i], 0, dynImports[i].length() - 2);
                }
                // Or we can have a precise match.
                else
                {
                    matches = pkgTarget.equals(dynImports[i]);
                }
            }

            // If the target package does not match any dynamically imported
            // packages or if the module already imports the target package,
            // then just return null. The module may already import the target
            // package if the class being searched for does not actually exist.
            if (!matches || ImportSearchPolicy.doesImport(module, pkgTarget))
            {
                return null;
            }

            // Try to add the import, which will also resolve the import
            // if the module is currently active.
            int[] version = { 0, 0, 0 };
            if (m_oscar.addImport(module, pkgTarget, version, false))
            {
                // Get the module that resolves the package so we can use
                // its class loader to load the target class; it is only
                // necessary to load the class manually this first time
                // because we have dynamically add the dynamically imported
                // package to the importing module's meta-data, so future
                // attempts to load classes from the dynamically imported
                // package will be processed in the normal fashion.
                Module resolvingModule =
                    ImportSearchPolicy.getImportResolvingModule(module, pkgTarget);
                if (resolvingModule != null)
                {
                    // Now try to get the resource from the exporter.
                    return resolvingModule.getClassLoader().getResource(name);
                }
            }
        }
        catch (Exception ex)
        {
            Oscar.error("Unable to dynamically import package.", ex);
        }

        return null;
    }

    /**
     * Utility method that returns the <tt>DYNAMIC_IMPORTS_ATTR</tt>
     * attribute for the specified module.
     * @param module the module whose <tt>DYNAMIC_IMPORTS_ATTR</tt>
     *        attribute is to be retrieved.
     * @return an <tt>String[]</tt>.
    **/
    public static String[] getDynamicImports(Module module)
    {
        Object value = module.getAttribute(OSGiImportSearchPolicy.DYNAMIC_IMPORTS_ATTR);
        if (value != null)
        {
            return (String[]) value;
        }
        return new String[0];
    }
}