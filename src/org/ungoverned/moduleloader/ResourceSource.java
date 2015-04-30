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

/**
 * <p>
 * This interface represents a source for obtaining resources for a
 * given module via the module's class loader. A resource source is used
 * for retrieving both classes and resources; at this level, classes are
 * treated in an identical manner as an ordinary resource. Resource sources
 * are completely arbitrary and implementations may load resources from a JAR
 * file, the network, a database, or anywhere.
 * </p>
 * <p>
 * All resource sources are initialized before first usage via a call
 * to the <a href="#open()"><tt>ResourceSource.open()</tt></a> method and
 * are also deinitialized via a call to
 * <a href="#open()"><tt>ResourceSource.close()</tt></a>. Resource sources
 * should be implemented such that they can be opened, closed, and then
 * re-opened.
 * </p>
 * @see org.ungoverned.moduleloader.Module
 * @see org.ungoverned.moduleloader.ModuleClassLoader
**/
public interface ResourceSource
{
    /**
     * <p>
     * This method initializes the resource source. It is called when
     * the associated module is added to the <tt>ModuleManager</tt>. It
     * is acceptable for implementations to ignore duplicate calls to this
     * method if the resource source is already opened.
     * </p>
    **/
    public void open();

    /**
     * <p>
     * This method de-initializes the resource source. It is called when
     * the associated module is removed from the <tt>ModuleManager</tt> or
     * when the module is reset by the <tt>ModuleManager</tt>.
     * </p>
    **/
    public void close();

    /**
     * <p>
     * This method returns a boolean indicating whether the resource source
     * contains the specified resource.
     * </p>
     * @param name the name of the resource whose existence is being checked.
     * @param <tt>true</tt> if the resource source has the resource, <tt>false</tt>
     *        otherwise.
     * @throws java.lang.IllegalStateException if the resource source has not
     *         been opened.
    **/
    public boolean hasResource(String name) throws IllegalStateException;

    /**
     * <p>
     * This method returns a byte array of the specified resource's contents.
     * </p>
     * @param name the name of the resource to retrieve.
     * @param a byte array of the resource's contents or <tt>null</tt>
     *        if the resource was not found.
     * @throws java.lang.IllegalStateException if the resource source has not
     *         been opened.
    **/
    public byte[] getBytes(String name) throws IllegalStateException;
}