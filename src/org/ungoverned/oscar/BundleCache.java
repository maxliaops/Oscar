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

import java.io.InputStream;

/**
 * <p>
 * This interface represents the storage mechanism that Oscar uses for
 * caching bundles. It is possible for multiple implementations of
 * this interface to exist for different storage technologies, such as the
 * file system, memory, or a database. Oscar includes a default implementation
 * of this interface that uses the file system. Oscar allows you to specify
 * alternative implementations to use by specifying a class name via the
 * <tt>oscar.cache.class</tt> system property. Bundle cache implemenations
 * should implement this interface and provide a default constructor.
 * </p>
 * @see org.ungoverned.oscar.BundleArchive
**/
public interface BundleCache
{
    /**
     * <p>
     * This method is called before using the BundleCache implementation
     * to initialize it and to pass it a reference to its associated
     * Oscar instance. The main purpose for passing the <tt>BundleCache</tt>
     * implementation a reference to its Oscar instance is to allow it
     * to use <tt>Oscar.getConfigProperty()</tt> to access system property values;
     * the <tt>BundleCache</tt> implementation should not use
     * <tt>System.getProperty()</tt> directly. <tt>Oscar.getConfigProperty()</tt>
     * provides access to properties passed into the Oscar instance's
     * constructor. If no properties were passed in to the constructor
     * then it searches <tt>System.getProperty()</tt>. This approach allows
     * multiple instances of Oscar to exist in memory at the same time, but for
     * them to be configured differently. For example, an application may
     * want two instances of Oscar, where each instance stores their cache
     * in a different location in the file system. When using multiple
     * instances of Oscar in memory at the same time, system properties
     * should be avoided and all properties should be passed in to Oscar's
     * constructor.
     * </p>
     * @param oscar the Oscar instance associated with the bundle cache.
     * @throws Exception if any error occurs.
    **/
    public void initialize(Oscar oscar)
        throws Exception;

    /**
     * <p>
     * Returns all cached bundle archives.
     * </p>
     * @return an array of all cached bundle archives.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive[] getArchives()
        throws Exception;

    /**
     * <p>
     * Returns the bundle archive associated with the specified
     * bundle indentifier.
     * </p>
     * @param id the identifier of the bundle archive to retrieve.
     * @return the bundle archive assocaited with the specified bundle identifier.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive getArchive(long id)
        throws Exception;

    /**
     * <p>
     * Creates a new bundle archive for the specified bundle
     * identifier using the supplied location string and input stream. The
     * contents of the bundle JAR file should be read from the supplied
     * input stream, which will not be <tt>null</tt>. The input stream is
     * closed by the caller; the implementation is only responsible for
     * closing streams it opens. If this method completes successfully, then
     * it means that the initial bundle revision of the specified bundle was
     * successfully cached.
     * </p>
     * @param id the identifier of the bundle associated with the new archive.
     * @param location the location of the bundle associated with the new archive.
     * @param is the input stream to the bundle's JAR file.
     * @return the created bundle archive.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive create(long id, String location, InputStream is)
        throws Exception;

    /**
     * <p>
     * Saves an updated revision of the specified bundle to
     * the bundle cache using the supplied input stream. The contents of the
     * updated bundle JAR file should be read from the supplied input stream,
     * which will not be <tt>null</tt>. The input stream is closed by the
     * caller; the implementation is only responsible for closing streams
     * it opens. Updating a bundle in the cache does not replace the current
     * revision of the bundle, it makes a new revision available. If this
     * method completes successfully, then it means that the number of
     * revisions of the specified bundle has increased by one.
     * </p>
     * @param ba the bundle archive of the bundle to update.
     * @param is the input stream to the bundle's updated JAR file.
     * @throws Exception if any error occurs.
    **/
    public void update(BundleArchive ba, InputStream is)
        throws Exception;

    /**
     * <p>
     * Purges all old revisions of the specified bundle from
     * the cache. If this method completes successfully, then it means that
     * only the most current revision of the bundle should exist in the cache.
     * </p>
     * @param ba the bundle archive of the bundle to purge.
     * @throws Exception if any error occurs.
    **/
    public void purge(BundleArchive ba)
        throws Exception;

    /**
     * <p>
     * Removes the specified bundle from the cache. If this method
     * completes successfully, there should be no trace of the removed bundle
     * in the cache.
     * </p>
     * @param ba the bundle archive of the bundle to remove.
     * @throws Exception if any error occurs.
    **/
    public void remove(BundleArchive ba)
        throws Exception;
}