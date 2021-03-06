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
package org.ungoverned.oscar.util;

import java.util.EventListener;

import org.osgi.framework.Bundle;

public class ListenerWrapper
{
    // The bundle associated with the listener.
    private Bundle m_bundle = null;
    // Listener class.
    private Class m_class = null;
    // The original listener.
    private EventListener m_listener = null;

    public ListenerWrapper(Bundle bundle, Class clazz, EventListener l)
    {
        m_bundle = bundle;
        m_class = clazz;
        m_listener = l;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    public Class getListenerClass()
    {
        return m_class;
    }

    public EventListener getListener()
    {
        return m_listener;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof ListenerWrapper)
        {
            return (((ListenerWrapper) obj).m_listener == m_listener);
        }
        else if (obj instanceof EventListener)
        {
            return (obj == m_listener);
        }
        return false;
    }
}
