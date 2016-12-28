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

import ij.plugin.PlugIn;
import ij.Prefs;
import javax.swing.JOptionPane;

public class Dcm4chee implements PlugIn 
{
  public void run(String arg) 
  {
    SelectImageDialog d = new SelectImageDialog();
    d.Unserialize(Prefs.get("Dcm4chee.servers", ""));

    boolean success = d.ShowModal();
    Prefs.set("Dcm4chee.servers", d.Serialize());  

    if (success)
    {
      try 
      {
        DicomDecoder decoder = new DicomDecoder(d.GetSelectedConnection(), d.IsInstanceSelected(), d.GetSelectedUuid());
        decoder.GetImage().show();
      }
      catch (Exception e) 
      {
        JOptionPane.showMessageDialog(null, "Error while importing this image: " + e.getMessage());
        //e.printStackTrace(System.out);
      }
    }
  }
}
