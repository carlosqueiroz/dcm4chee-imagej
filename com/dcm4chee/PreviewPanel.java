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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

public class PreviewPanel extends JPanel
{
  private BufferedImage image_ = null;

  public void Reset()
  {
    image_ = null;
    repaint();
  }

  public void Load(Dcm4cheeConnection Dcm4chee,
                   String uri)
  {
    try 
    {         
      image_ = Dcm4chee.ReadImage(uri);
      repaint();
    }
    catch (IOException e) 
    {
      Reset();
    }
  }

  @Override
  protected void paintComponent(Graphics g) 
  {
    super.paintComponent(g);

    if (image_ != null)
    {
      float scaleX = (float) getWidth() / (float) image_.getWidth();
      float scaleY = (float) getHeight() / (float) image_.getHeight();
      float scale = Math.min(scaleX, scaleY);
      if (scale > 1)
      {
        // Do not upscale the image
        scale = 1;
      }

      int width = Math.round((float) image_.getWidth() * scale);
      int height = Math.round((float) image_.getHeight() * scale);

      int dx = (getWidth() - width) / 2;
      int dy = (getHeight() - height) / 2;
      g.drawImage(image_, dx, dy, dx + width, dy + height, 
                  0, 0, image_.getWidth(), image_.getHeight(), null);
    }
  }
}
