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

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.ungoverned.oscar.ldap.*;
import org.ungoverned.oscar.util.CaseInsensitiveMap;

/**
 * This class implements an RFC 1960-based filter. The syntax of the
 * filter string is the string representation of LDAP search filters
 * as defined in RFC 1960. These filters are used to search for services
 * and to track services using <tt>ServiceTracker</tt> objects.
**/
class FilterImpl implements Filter
{
    private String m_toString = null;
    private Evaluator m_evaluator = null;
    private SimpleMapper m_mapper = null;

    /**
     * Construct a filter for a given filter expression string.
     * @param expr the filter expression string for the filter.
    **/
    public FilterImpl(String expr) throws InvalidSyntaxException
    {
        if (expr == null)
        {
            throw new InvalidSyntaxException("Filter cannot be null", null);
        }

        if (expr != null)
        {
            CharArrayReader car = new CharArrayReader(expr.toCharArray());
            LdapLexer lexer = new LdapLexer(car);
            Parser parser = new Parser(lexer);
            try
            {
                if (!parser.start())
                {
                    throw new InvalidSyntaxException(
                        "Failed to parse LDAP query.", expr);
                }
            }
            catch (ParseException ex)
            {
                throw new InvalidSyntaxException(
                    ex.getMessage(), expr);
            }
            catch (IOException ex)
            {
                throw new InvalidSyntaxException(
                    ex.getMessage(), expr);
            }
            m_evaluator = new Evaluator(parser.getProgram());
            m_mapper = new SimpleMapper();
        }
    }

    /**
     * Compares the <tt>Filter</tt> object to another.
     * @param o the object to compare this <tt>Filter</tt> against.
     * @return If the other object is a <tt>Filter</tt> object, it
     *         returns <tt>this.toString().equals(obj.toString())</tt>;
     *         <tt>false</tt> otherwise.
    **/
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        else if (o instanceof Filter)
        {
            return toString().equals(o.toString());
        }
        return false;
    }

    /**
     * Returns the hash code for the <tt>Filter</tt> object.
     * @return The value <tt>this.toString().hashCode()</tt>.
    **/
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Filter using a <tt>Dictionary</tt> object. The <tt>Filter</tt>
     * is executed using the <tt>Dictionary</tt> object's keys and values.
     * @param dict the <tt>Dictionary</tt> object whose keys and values
     *             are used to determine a match.
     * @return <tt>true</tt> if the <tt>Dictionary</tt> object's keys
     *         and values match this filter; <tt>false</tt> otherwise.
     * @throws IllegalArgumentException if the dictionary contains case
     *         variants of the same key name.
    **/
    public boolean match(Dictionary dict)
        throws IllegalArgumentException
    {
        try
        {
            m_mapper.setSource(dict);
            return m_evaluator.evaluate(m_mapper);
        }
        catch (AttributeNotFoundException ex)
        {
            Oscar.debug("FilterImpl: " + ex);
        }
        catch (EvaluationException ex)
        {
            Oscar.error("FilterImpl: " + toString(), ex);
        }
        return false;
    }

    /**
     * Filter using a service's properties. The <tt>Filter</tt>
     * is executed using the properties of the referenced service.
     * @param ref A reference to the service whose properties
     *             are used to determine a match.
     * @return <tt>true</tt> if the service's properties match this
     *         filter; <tt>false</tt> otherwise.
    **/
    public boolean match(ServiceReference ref)
    {
        try
        {
            m_mapper.setSource(ref);
            return m_evaluator.evaluate(m_mapper);
        }
        catch (AttributeNotFoundException ex)
        {
            Oscar.debug("FilterImpl: " + ex);
        }
        catch (EvaluationException ex)
        {
            Oscar.error("FilterImpl: " + toString(), ex);
        }
        return false;
    }

    /**
     * Returns the <tt>Filter</tt> object's filter string.
     * @return Filter string.
    **/
    public String toString()
    {
        if (m_toString == null)
        {
            m_toString = m_evaluator.toStringInfix();
        }
        return m_toString;
    }

    static class SimpleMapper implements Mapper
    {
        private ServiceReference m_ref = null;
        private Map m_map = null;

        public void setSource(ServiceReference ref)
        {
            m_ref = ref;
            m_map = null;
        }

        public void setSource(Dictionary dict)
        {
            if (m_map == null)
            {
                m_map = new CaseInsensitiveMap();
            }
            else
            {
                m_map.clear();
            }

            if (dict != null)
            {
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements())
                {
                    Object key = keys.nextElement();
                    if (m_map.get(key) == null)
                    {
                        m_map.put(key, dict.get(key));
                    }
                    else
                    {
                        throw new IllegalArgumentException(
                            "Duplicate attribute: " + key.toString());
                    }
                }
            }
            m_ref = null;
        }

        public Object lookup(String name)
        {
            if (m_map == null)
            {
                return m_ref.getProperty(name);
            }
            return m_map.get(name);
        }
    }
}