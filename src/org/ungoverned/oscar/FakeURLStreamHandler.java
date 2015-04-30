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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * This class implements a fake stream handler. This class is necessary in
 * some cases when assigning <tt>CodeSource</tt>s to classes in
 * <tt>BundleClassLoader</tt>. In general, the bundle location is an URL
 * and this URL is used as the code source for the bundle's associated
 * classes. The OSGi specification does not require that the bundle
 * location be an URL, though, so in that case we try to generate a
 * fake URL for the code source of the bundle, which is just the location
 * string prepended with the "location:" protocol, by default. We need
 * this fake handler to avoid an unknown protocol exception.
**/
class FakeURLStreamHandler extends URLStreamHandler
{
    protected URLConnection openConnection(URL url) throws IOException
    {
        return null;
    }
}
