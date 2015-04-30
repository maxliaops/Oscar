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
package org.ungoverned.moduleloader;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * <p>
 * The <tt>Module</tt> class is a grouping mechanism for application classes
 * and resources. Conceptually, most applications are grouped into
 * entities such as JAR files (containing classes and resources) and native
 * libraries. In some cases, these entities are core application classes and
 * resources, while in other cases, these entities are ancillary, such as
 * dynamically loaded plug-ins. Applications place some level of semantics
 * onto these types of entities or <i>modules</i>, but for the <tt>ModuleLoader</tt>,
 * no particular semantics are attached to modules (other than they are a grouping
 * mechanism for classes and resources). This means that the application
 * is free to map itself into modules any way that is appropriate.
 * </p>
 * <p>
 * A module has the following features:
 * </p>
 * <ul>
 *   <li>A unique identifier within the scope of its <tt>ModuleManager</tt>.
 *   </li>
 *   <li>A set of key-value attribute pairs.
 *   </li>
 *   <li>A set of resource sources from which it is possible to
 *       retrieve classes and resources.
 *   </li>
 *   <li>A set of native library sources from which it is possible
 *       to retrieve native libraries.
 *   </li>
 * </ul>
 * <p>
 * A module's identifier must be unique within the scope of its
 * <tt>ModuleManager</tt>, but there is no meaning associated with it. The
 * set of attribute-value pairs attached to the module have no meaning to
 * the <tt>ModuleManager</tt>, nor does it consult them at all. The point
 * of these attributes is to attach meta-data for use by
 * <a href="SearchPolicy.html"><tt>SearchPolicy</tt></a> implementations.
 * Attributes are represented as an array of <tt>Object</tt>
 * arrays, i.e., <tt>Object[][]</tt>. Each element in the attribute array is
 * a two-element <tt>Object</tt> array, where <tt>Module.KEY_IDX</tt> is the attribute's
 * key and <tt>Module.VALUE_IDX</tt> is the attribute's value.
 * </p>
 * <p>
 * The actual contents of a module is contained in two sets of sources
 * for its resources and native libraries,
 * <a href="ResourceSource.html"><tt>ResourceSource</tt></a>s
 * and <a href="LibrarySource.html"><tt>LibrarySource</tt></a>s, respectively.
 * Each module also has a <a href="ModuleClassLoader.html"><tt>ModuleClassLoader</tt></a>
 * associated with it. The <tt>ModuleClassLoader</tt> consults these two types
 * of sources to find classes, resources, and native libraries.
 * </p>
 * @see org.ungoverned.moduleloader.ModuleManager
 * @see org.ungoverned.moduleloader.ModuleClassLoader
 * @see org.ungoverned.moduleloader.ResourceSource
 * @see org.ungoverned.moduleloader.LibrarySource
**/
public class Module
{
    /**
     * This is the index used to retrieve the key of an attribute;
     * an attribute is represented as an <tt>Object[]</tt> instance.
    **/
    public static final int KEY_IDX = 0;
    /**
     * This is the index used to retrieve the value of an attribute;
     * an attribute is represented as an <tt>Object[]</tt> instance.
    **/
    public static final int VALUE_IDX = 1;

    private ModuleManager m_mgr = null;
    private String m_id = null;
    private Map m_attributeMap = new HashMap();
    private ResourceSource[] m_resSources = null;
    private LibrarySource[] m_libSources = null;
    private ModuleClassLoader m_loader = null;

    /**
     * <p>
     * Constructs a <tt>Module</tt> instance that will be associated with
     * the specified <tt>ModuleManager</tt> and will have the specified
     * identifier, attributes, resource sources, and library sources. In general,
     * modules should not be created directly, but should be created by making
     * a call to <tt>ModuleManager.addModule()</tt>.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> that will be associated to
     *       the instance.
     * @param id the identifier of the instance.
     * @param attributes the set of attributes associated with the instance.
     * @param resSources the set of <tt>ResourceSource</tt>s associated with
     *        the instance.
     * @param libSources the set of <tt>LibrarySource</tt>s associated with
     *        the instance.
     * @see org.ungoverned.moduleloader.ModuleManager
     * @see org.ungoverned.moduleloader.ResourceSource
     * @see org.ungoverned.moduleloader.LibrarySource
    **/
    public Module(
        ModuleManager mgr, String id, Object[][] attributes,
        ResourceSource[] resSources, LibrarySource[] libSources)
    {
        m_mgr = mgr;
        m_id = id;
        initialize(attributes, resSources, libSources);
    }

    /**
     * <p>
     * Returns the identifier of the module.
     * </p>
     * @return the identifier of the module.
    **/
    public String getId()
    {
        return m_id;
    }

    /**
     * <p>
     * Returns the attribute set associated with this module. Attributes
     * are represented as an array of <tt>Object</tt> arrays, i.e.,
     * <tt>Object[][]</tt>. Each element in the attribute array is
     * two-element <tt>Object</tt> array, where <tt>Module.KEY_IDX</tt>
     * is the index to the attribute key and <tt>Module.VALUE_IDX</tt>
     * is the index to the attribute value. The returned array is a
     * copy and may be freely modified.
     * </p>
     * @return the attribute set associated with this module.
    **/
    public synchronized Object[][] getAttributes()
    {
        Set s = m_attributeMap.entrySet();
        Object[][] attributes = new Object[s.size()][];
        Iterator iter = s.iterator();
        for (int i = 0; iter.hasNext(); i++)
        {
            Map.Entry entry = (Map.Entry) iter.next();
            attributes[i] = new Object[] { entry.getKey(), entry.getValue() };
        }
        return attributes;
    }

    /**
     * <p>
     * Returns the attribute value associated with the specified key.
     * </p>
     * @param key the key of the attribute whose value is to be retrieved.
     * @return the attribute's value or <tt>null</tt>.
    **/
    public synchronized Object getAttribute(String key)
    {
        return m_attributeMap.get(key);
    }

    /**
     * <p>
     * Sets the attribute value associated with the specified key. The
     * attribute will be added if it does not currently exist.
     * </p>
     * @param key the key of the attribute whose value is to be set.
     * @param value the new value to be associated with the attribute key.
    **/
    public synchronized void setAttribute(String key, Object value)
    {
        m_attributeMap.put(key, value);
    }

    /**
     * <p>
     * Returns the array of <tt>ResourceSource</tt>s associated with
     * the module. The returned array is not a copy and therefore should
     * not be modified.
     * </p>
     * @return the array of <tt>ResourceSource</tt>s associated with
     *         the module.
     * @see org.ungoverned.moduleloader.ResourceSource
    **/
    public ResourceSource[] getResourceSources()
    {
        return m_resSources;
    }

    /**
     * <p>
     * Returns the array of <tt>LibrarySource</tt>s associated with
     * the module. The returned array is not a copy and therefore should
     * not be modified.
     * </p>
     * @return the array of <tt>LibrarySource</tt>s associated with
     *         the module.
     * @see org.ungoverned.moduleloader.LibrarySource
    **/
    public LibrarySource[] getLibrarySources()
    {
        return m_libSources;
    }

    /**
     * <p>
     * Returns the <tt>ModuleClassLoader</tt> associated with this module.
     * If a security manager is installed, then this method uses a privileged
     * action to avoid a security exception being thrown to the caller.
     * </p>
     * @return the <tt>ModuleClassLoader</tt> associated with this module.
     * @see org.ungoverned.moduleloader.ModuleClassLoader
    **/
    public synchronized ModuleClassLoader getClassLoader()
    {
        if (m_loader == null)
        {
            if (System.getSecurityManager() != null)
            {
                m_loader = (ModuleClassLoader) AccessController.doPrivileged(
                    new GetClassLoaderPrivileged(m_mgr, this));
            }
            else
            {
                m_loader = new ModuleClassLoader(m_mgr, this);
            }
        }

        return m_loader;
    }

    /**
     * <p>
     * Returns the module's identifier.
     * </p>
     * @return the module's identifier.
    **/
    public String toString()
    {
        return m_id;
    }

    /**
     * <p>
     * Resets the module by throwing away its associated class loader and
     * re-initializing its attributes, resource sources, and library sources
     * with the specified values.
     * </p>
     * @param attributes the new attributes to be associated with the module.
     * @param resSources the new resource sources to be associated with the module.
     * @param libSources the new library sources to be associated with the module.
     * @see org.ungoverned.moduleloader.ResourceSource
     * @see org.ungoverned.moduleloader.LibrarySource
    **/
    protected synchronized void reset(
        Object[][] attributes, ResourceSource[] resSources,
        LibrarySource[] libSources)
    {
        // Throw away class loader.
        m_loader = null;
        // Clear attribute map.
        m_attributeMap.clear();
        // Close all sources.
        dispose();
        // Re-initialize.
        initialize(attributes, resSources, libSources);
    }

    /**
     * <p>
     * Disposes the module by closing all resource and library sources.
     * </p>
    **/
    protected synchronized void dispose()
    {
        // Close sources.
        for (int i = 0; (m_resSources != null) && (i < m_resSources.length); i++)
        {
            m_resSources[i].close();
        }
        for (int i = 0; (m_libSources != null) && (i < m_libSources.length); i++)
        {
            m_libSources[i].close();
        }
    }

    /**
     * <p>
     * Initializes the module by copying the specified attribute array into
     * a map and opening all resource and library sources.
     * </p>
     * @param attributes the attributes to be put into a map.
     * @param resSources the resource sources to be opened.
     * @param libSources the library sources to be opened.
     * @see org.ungoverned.moduleloader.ResourceSource
     * @see org.ungoverned.moduleloader.LibrarySource
    **/
    private void initialize(
        Object[][] attributes, ResourceSource[] resSources, LibrarySource[] libSources)
    {
        for (int i = 0; (attributes != null) && (i < attributes.length); i++)
        {
            m_attributeMap.put(attributes[i][KEY_IDX], attributes[i][VALUE_IDX]);
        }

        m_resSources = resSources;
        m_libSources = libSources;

        // Open sources.
        for (int i = 0; (m_resSources != null) && (i < m_resSources.length); i++)
        {
            m_resSources[i].open();
        }
        for (int i = 0; (m_libSources != null) && (i < m_libSources.length); i++)
        {
            m_libSources[i].open();
        }
    }

    private static class GetClassLoaderPrivileged implements PrivilegedAction
    {
        private ModuleManager m_mgr = null;
        private Module m_module = null;

        public GetClassLoaderPrivileged(ModuleManager mgr, Module module)
        {
            m_mgr = mgr;
            m_module = module;
        }

        public Object run()
        {
            return new ModuleClassLoader(m_mgr, m_module);
        }
    }
}