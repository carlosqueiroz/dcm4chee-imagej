/**
 * 
 * 
 * 
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/


package com.Dcm4chee;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.json.simple.JSONObject;


public class Dcm4cheeConfigurationDialog extends JDialog 
{
  private JTextField name_ = new JTextField("Dcm4chee");
  private JTextField url_ = new JTextField("http://localhost:8080/dcm4chee-arc/ui");
  private JTextField username_ = new JTextField("");
  private JTextField password_ = new JPasswordField("");
  private boolean isSuccess_;

  private Dcm4cheeConnection CreateConnection()
  {
    Dcm4cheeConnection Dcm4chee = new Dcm4cheeConnection();
    Dcm4chee.SetInsecure(true);  // Fix issue 9 (cannot connect to self-signed certificates)
    Dcm4chee.SetName(name_.getText());
    Dcm4chee.SetBaseUrl(url_.getText());

    if (username_.getText().length() != 0)
    {
      Dcm4chee.SetCredentials(username_.getText(), password_.getText());
    }

    return Dcm4chee;
  }

  public Dcm4cheeConfigurationDialog()
  {
    final JPanel contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(20, 5, 5, 5));
		contentPanel.setLayout(new GridLayout2(0, 2, 20, 5));

    JLabel label = new JLabel("Name:");
    label.setHorizontalAlignment(JLabel.RIGHT);
    contentPanel.add(label);
    contentPanel.add(name_);

    label = new JLabel("URL:");
    label.setHorizontalAlignment(JLabel.RIGHT);
    contentPanel.add(label);
    contentPanel.add(url_);

    label = new JLabel("Username:");
    label.setHorizontalAlignment(JLabel.RIGHT);
    contentPanel.add(label);
    contentPanel.add(username_);

    label = new JLabel("Password:");
    label.setHorizontalAlignment(JLabel.RIGHT);
    contentPanel.add(label);
    contentPanel.add(password_);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(contentPanel, BorderLayout.NORTH);

    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
		{
			{
        JButton test = new JButton("Test connection");
				test.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            Dcm4cheeConnection Dcm4chee = CreateConnection();
            try
            {
              JSONObject system = (JSONObject) Dcm4chee.ReadJson("system");
              JOptionPane.showMessageDialog(null, "Successfully connected to this Dcm4chee server " +
                                            "(version: " + (String) system.get("Version") + ")!", 
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            catch (IOException e)
            {
              JOptionPane.showMessageDialog(null, "Cannot connect to this Dcm4chee server!", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        });
				buttonPane.add(test);
			}
			{
        JButton okButton = new JButton("Add");
				okButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            isSuccess_ = true;
            setVisible(false);
          }
        });
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg) {
            setVisible(false);
					}
				});
				buttonPane.add(cancelButton);
			}
		}

    setUndecorated(false);
    setSize(500,500);
    setTitle("Add new server");
    setModal(true);
  }



  public Dcm4cheeConnection ShowModal()
  {
    isSuccess_ = false;
    setVisible(true);
    if (isSuccess_)
    {
      return CreateConnection();
    }
    else
    {
      return null;
    }
  }

}
