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
package org.ungoverned.oscar.installer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.*;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PropertyPanel extends JPanel
{
    private List m_propList = null;
    private Map m_propToCompMap = null;

    public PropertyPanel(List paramList)
    {
        super();
        m_propList = paramList;
        m_propToCompMap = new HashMap();
        layoutComponents();
    }

    public void setEnabled(boolean b)
    {
        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            JComponent comp = (JComponent) m_propToCompMap.get(prop.getName());
            comp.setEnabled(b);
        }
    }

    public List getProperties()
    {
        return m_propList;
    }

    protected void layoutComponents()
    {
        // Create the field panel for entering query variables.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        setLayout(grid);

        for (int i = 0; i < m_propList.size(); i++)
        {
            Property prop = (Property) m_propList.get(i);
            JLabel label = null;
            JComponent component = null;

            // Add label.
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.gridheight = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.EAST;
            grid.setConstraints(label = new JLabel(prop.getName()), gbc);
            add(label);

            gbc.gridx = 1;
            gbc.gridy = i;
            gbc.gridheight = 1;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.WEST;
            grid.setConstraints(component = prop.getEditor(), gbc);
            add(component);

            m_propToCompMap.put(prop.getName(), component);
        }
    }
}