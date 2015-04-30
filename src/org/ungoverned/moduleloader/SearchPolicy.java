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

import java.net.URL;

/**
 * <p>
 * This interface represents a policy to define the most basic behavior
 * of how classes, resources, and native libraries within a specific instance
 * of <tt>ModuleManager</tt> are found. A <tt>ModuleManager</tt> manages a set of
 * <tt>Module</tt>s, each of which is a potential source of classes, resources,
 * and native libraries. The search policy makes it possible to consult these
 * sources without hard-coding assumptions about application behavior
 * or structure. Applicaitons inject their own specific class loading policy
 * by creating a custom search policy or by selecting a pre-existing search
 * policy that matches their needs.
 * </p>
 * <p>
 * The search policy is used by <tt>ModuleClassLoader</tt>, of which, there
 * is one per <tt>Module</tt> within a given <tt>ModuleManager</tt> instance.
 * The search policy is consulted by the <tt>ModuleClassLoader</tt> whenever
 * there is a request for a class, resource, or native library. The search
 * policy will generally search other modules in an application-specific
 * way in order to find the requested item; for example, an application may
 * use a policy where module's may import from one another. If the search
 * policy provides an answer, then the <tt>ModuleClassLoader</tt> will use
 * this to answer the originating request.
 * </p>
 * <p>
 * <b><i>Important:</i></b> The search policy <i>searches</i> modules in
 * some application-specific manner in order to find a class or resource.
 * This <i>search</i> is instigated, either directly or indirectly, by calls
 * to <tt>ModuleClassLoader.loadClass()</tt> and <tt>ModuleClassLoader.getResource()</tt>,
 * respectively. In order for the search policy to load a class or resource,
 * it must <b>not</b> use <tt>ModuleClassLoader.loadClass()</tt> or
 * <tt>ModuleClassLoader.getResource()</tt> again, because this would result
 * in an infinite loop. Instead, the <tt>ModuleClassLoader</tt> offers the
 * the methods <tt>ModuleClassLoader.searchForClass()</tt> and
 * <tt>ModuleClassLoader.searchForResource()</tt> to search a given module
 * and to avoid an infinite loop.
 * </p>
 * <pre>
 *     ...
 *     public Class findClass(Module module, String name)
 *     {
 *         Module[] modules = m_mgr.getModules();
 *         for (int i = 0; i < modules.length; i++)
 *         {
 *             try {
 *                 Class clazz = modules[i].getClassLoader().searchForClass(name);
 *                 if (clazz != null)
 *                 {
 *                     return clazz;
 *                 }
 *             } catch (Throwable th) {
 *             }
 *         }
 *
 *         return null;
 *     }
 *     ...
 * </pre>
 * <p>
 * In the above code, the search policy "exhaustively" searches every module in the
 * <tt>ModuleManager</tt> to find the requested resource. Note that this policy
 * will also search the module that originated the request, which is not totally
 * necessary since returning <tt>null</tt> will cause the <tt>ModuleClassLoader</tt>
 * to search the originating module's <tt>ResourceSource</tt>s.
 * </p>
**/
public interface SearchPolicy
{
    /**
     * <p>
     * This method is called once by the <tt>ModuleManager</tt> to
     * give the search policy instance a reference to its associated
     * module manager. This method should be implemented such that
     * it cannot be called twice; calling this method a second time
     * should produce an illegal state exception.
     * </p>
     * @param mgr the module manager associated with this search policy.
     * @throws java.lang.IllegalStateException if the method is called
     *         more than once.
    **/
    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException;

    /**
     * <p>
     * This method tries to find the specified class for the specified
     * module. How the class is found or whether it is actually retrieved
     * from the specified module is dependent upon the implementation. The
     * default <tt>ModuleClassLoader.loadClass()</tt> method does not do
     * any searching of its own, it merely calls <tt>ClassLoader.resolveClass()</tt>
     * on the class returned by this method.
     * </p>
     * <p>
     * This method may return <tt>null</tt> or throw an exception if the
     * specified class is not found. Whether a specific search policy
     * implementation should do one or the other depends on the details
     * of the specific search policy. The <tt>ModuleClassLoader</tt>
     * first delegates to this method and then to the local resource
     * sources of the module. If this method returns null, then the local
     * resource sources will be searched. On the other hand, if this method
     * throws an exception, then the local resource sources will not be
     * searched.
     * </p>
     * <p>
     * <b>Important:</b> If the implementation of this method delegates
     * the class loading to a <tt>ModuleClassLoader</tt> of another module,
     * then it should <b>not</b> use the method <tt>ModuleClassLoader.loadClass()</tt>
     * to load the class; it should use <tt>ModuleClassLoader.searchForClass()</tt>
     * instead. This is necessary to eliminate an infinite loop that would
     * occur otherwise. Also, with respect to the <tt>ModuleLoader</tt> framework,
     * this method will only be called by a single thread at a time and is only
     * intended to be called by <tt>ModuleClassLoader.loadClass()</tt>.
     * </p>
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return the class if found, <tt>null</tt> otherwise.
     * @throws java.lang.ClassNotFoundException if the class could not be
     *         found and the entire search operation should fail.
    **/
    public Class findClass(Module module, String name)
        throws ClassNotFoundException;

    /**
     * <p>
     * This method tries to find the specified resource for the specified
     * module. How the resource is found or whether it is actually retrieved
     * from the specified module is dependent upon the implementation. The
     * default <tt>ModuleClassLoader.getResource()</tt> method does not do
     * any searching on its own.
     * </p>
     * <p>
     * This method may return <tt>null</tt> or throw an exception if the
     * specified resource is not found. Whether a specific search policy
     * implementation should do one or the other depends on the details
     * of the specific search policy. The <tt>ModuleClassLoader</tt>
     * first delegates to this method and then to the local resource
     * sources of the module. If this method returns null, then the local
     * resource sources will be searched. On the other hand, if this method
     * throws an exception, then the local resource sources will not be
     * searched.
     * </p>
     * <p>
     * <b>Important:</b> If the implementation of this method delegates
     * the resource loading to a <tt>ModuleClassLoader</tt> of another module,
     * then it should not use the method <tt>ModuleClassLoader.getResource()</tt>
     * to get the resource; it should use <tt>ModuleClassLoader.searchForResource()</tt>
     * instead. This is necessary to eliminate an infinite loop that would
     * occur otherwise. Also, with respect to the <tt>ModuleLoader</tt> framework,
     * this method will only be called by a single thread at a time and is not
     * intended to be called directly.
     * </p>
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the resource.
     * @param name the name of the resource being loaded.
     * @return a <tt>URL</tt> to the resource if found, <tt>null</tt> otherwise.
     * @throws org.ungoverned.moduleloader.ResourceNotFoundException if the
     *         resource could not be found and the entire search operation
     *         should fail.
    **/
    public URL findResource(Module module, String name)
        throws ResourceNotFoundException;
}