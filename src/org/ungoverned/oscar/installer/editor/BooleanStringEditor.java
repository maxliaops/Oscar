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
package org.ungoverned.oscar.installer.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;

import javax.swing.*;

import org.ungoverned.oscar.installer.BooleanProperty;
import org.ungoverned.oscar.installer.Property;
import org.ungoverned.oscar.installer.StringProperty;

public class BooleanStringEditor extends JPanel
{
    private Property m_prop = null;
    private JCheckBox m_includeButton = null;
    private JTextField m_textField = null;

    public BooleanStringEditor(Property prop)
    {
        if ((prop instanceof BooleanProperty) && (prop instanceof StringProperty))
        {
            m_prop = prop;
        }
        else
        {
            throw new IllegalArgumentException(
                "Property must implement both boolean and string property interfaces.");
        }
        init();
    }

    public Property getProperty()
    {
        return m_prop;
    }

    public void setEnabled(boolean b)
    {
        m_includeButton.setEnabled(b);
        m_textField.setEnabled(b && m_includeButton.isSelected());
    }

    protected void init()
    {
        // Set layout.
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 2, 0, 2);
        setLayout(grid);

        // Add button.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        m_includeButton = new JCheckBox("");
        grid.setConstraints(m_includeButton, gbc);
        add(m_includeButton);

        // Add field.
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        m_textField = new JTextField(30);
        m_textField.setText(((StringProperty) m_prop).getStringValue());
        grid.setConstraints(m_textField, gbc);
        add(m_textField);
        m_textField.setEnabled(false);

        // Add action listener.
        m_includeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event)
            {
                if (m_includeButton.isSelected())
                {
                    ((BooleanProperty) m_prop).setBooleanValue(true);
                    m_textField.setEnabled(true);
                }
                else
                {
                    ((BooleanProperty) m_prop).setBooleanValue(false);
                    m_textField.setEnabled(false);
                }
            }
        });

        // Add focus listener.
        m_textField.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent event)
            {
            }
            public void focusLost(FocusEvent event)
            {
                if (!event.isTemporary())
                {
                    ((StringProperty) m_prop).setStringValue(m_textField.getText());
                }
            }
        });

        // Currently, the button is not selected. If the property
        // is true, then click once to select button.        
        if (((BooleanProperty) m_prop).getBooleanValue())
        {
            m_includeButton.doClick();
        }
    }
}