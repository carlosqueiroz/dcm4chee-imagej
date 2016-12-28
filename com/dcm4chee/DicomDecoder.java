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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.ColorProcessor;
import ij.io.FileInfo;
import ij.measure.Calibration;
import org.json.simple.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.FlowLayout;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class DicomDecoder
{
  private static class ProgressDialog extends JFrame
  {
    private JProgressBar bar_ = new JProgressBar();
    private boolean canceled_ = false;
    
    public ProgressDialog(int count)
    {
      getContentPane().setLayout(new BorderLayout());
      bar_.setBorder(new EmptyBorder(20, 20, 20, 20));
      getContentPane().add(bar_, BorderLayout.NORTH);

      JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent arg) {
          canceled_ = true;
        }
      });
      buttonPane.add(cancelButton);

      bar_.setMaximum(0);
      bar_.setMaximum(count);

      setSize(500, 150);
      setTitle("Importing series from Dcm4chee");
      setLocationRelativeTo(null);  // Center dialog on screen
    }

    public void SetProgress(int value)
    {
      bar_.setValue(value);
    }

    public boolean IsCanceled()
    {
      return canceled_;
    }
  };

  private static void ExtractCalibration(ImagePlus image,
                                         JSONObject tags)
  {
    JSONObject rescaleIntercept = (JSONObject) tags.get("0028,1052");
    JSONObject rescaleSlope = (JSONObject) tags.get("0028,1053");
    if (rescaleIntercept != null &&
        rescaleSlope != null)
    {
      double[] coeff = {
        Float.valueOf((String) rescaleIntercept.get("Value")),
        Float.valueOf((String) rescaleSlope.get("Value"))
      };
      image.getCalibration().setFunction(Calibration.STRAIGHT_LINE, coeff, "Gray Value");
    }
  }

  private static void ExtractPixelSpacing(ImagePlus image,
                                          JSONObject tags)
  {
    JSONObject pixelSpacing = (JSONObject) tags.get("0028,0030");
    if (pixelSpacing != null)
    {
      String[] tokens = ((String) pixelSpacing.get("Value")).split("\\\\");
      if (tokens.length == 2)
      {
        FileInfo fi = image.getFileInfo();
        fi.pixelWidth = Float.valueOf(tokens[0]);
        fi.pixelHeight = Float.valueOf(tokens[1]);
        fi.unit = "mm";

        image.setFileInfo(fi);
        image.getCalibration().pixelWidth = fi.pixelWidth;
        image.getCalibration().pixelHeight = fi.pixelHeight;
        image.getCalibration().setUnit(fi.unit);
      }
    }
  }

  private static void ExtractDicomInfo(ImagePlus image,
                                       JSONObject tags)
  {
    String info = new String();

    ArrayList<String> tagsIndex = new ArrayList<String>();
    for (Object tag : tags.keySet())
    {
      tagsIndex.add((String) tag);
    }

    Collections.sort(tagsIndex);
    for (String tag : tagsIndex) 
    {
      JSONObject value = (JSONObject) tags.get(tag);

      if (((String) value.get("Type")).equals("String"))
      {
        info += (tag + " " + (String) value.get("Name") +
                 ": " + (String) value.get("Value") + "\n");
      }
    }

    image.setProperty("Info", info);
  }

  private static ImageProcessor DecodeInstance(Dcm4cheeConnection c,
                                               String uuid) throws IOException
  {
    try
    {
      String uri = "/instances/" + uuid + "/image-uint16";
      ShortProcessor slice = new ShortProcessor(c.ReadImage(uri));
      return slice;
    }
    catch (IllegalArgumentException e)
    {
      // Color image
      String uri = "/instances/" + uuid + "/preview";
      ColorProcessor slice = new ColorProcessor(c.ReadImage(uri));
      return slice;
    }
  }

  private static ImageStack AddSlice(ImageStack stack,
                                     Dcm4cheeConnection c,
                                     String uuid) throws IOException
  {
    ImageProcessor slice = DecodeInstance(c, uuid);

    if (stack == null)
    {
      stack = new ImageStack(slice.getWidth(), slice.getHeight());
    }

    stack.addSlice("", slice);
    return stack;
  }


  static private class Slice implements Comparable
  {
    private Float index_;
    private String uuid_;
    
    Slice(float index,
          String uuid)
    {
      index_ = index;
      uuid_ = uuid;
    }

		@Override
    public int compareTo(Object other) 
		{
			return index_.compareTo(((Slice) other).index_);
    }

    public String GetUuid()
    {
      return uuid_;
    }
  }



  private String[] SortSlices(List<Slice> slices)
  {
    Collections.sort(slices);

    String[] result = new String[slices.size()];

    for (int i = 0; i < slices.size(); i++)
    {
      result[i] = slices.get(i).GetUuid();
    }

    return result;
  }



  private String[]  SortSlicesBy3D(Dcm4cheeConnection c, 
                                   JSONArray instances) throws IOException
  {
    ArrayList<Slice> slices = new ArrayList<Slice>();
    float normal[] = null;

    float minDistance = Float.POSITIVE_INFINITY;
    float maxDistance = Float.NEGATIVE_INFINITY;

    for (int i = 0; i < instances.size(); i++)
    {
      String uuid = (String) instances.get(i);
      JSONObject instance = (JSONObject) c.ReadJson("/instances/" + uuid + "/tags?simplify");
      if (!instance.containsKey("ImageOrientationPatient") ||
          !instance.containsKey("ImagePositionPatient"))
      {
        return null;
      }

      if (i == 0)
      {
        String[] tokens = ((String) instance.get("ImageOrientationPatient")).split("\\\\");
        if (tokens.length != 6)
        {
          return null;
        }

        float cosines[] = new float[6];
        for (int j = 0; j < 6; j++)
        {
          cosines[j] = Float.parseFloat(tokens[j]);
        }

        normal = new float[] {
          cosines[1] * cosines[5] - cosines[2] * cosines[4],
          cosines[2] * cosines[3] - cosines[0] * cosines[5],
          cosines[0] * cosines[4] - cosines[1] * cosines[3]
        };
      }

      String[] tokens = ((String) instance.get("ImagePositionPatient")).split("\\\\");
      if (tokens.length != 3)
      {
        return null;
      }

      float distance = 0;
      for (int j = 0; j < 3; j++)
      {
        distance += normal[j] * Float.parseFloat(tokens[j]);
      }

      minDistance = Math.min(minDistance, distance);
      maxDistance = Math.max(minDistance, distance);
      slices.add(new Slice(distance, uuid));
    }

    if (maxDistance - minDistance < 0.001)
    {
      return null;
    }

    return SortSlices(slices);
  }


  private String[]  SortSlicesByNumber(Dcm4cheeConnection c, 
                                       JSONArray instances) throws IOException
  {
    ArrayList<Slice> slices = new ArrayList<Slice>();

    for (int i = 0; i < instances.size(); i++)
    {
      String uuid = (String) instances.get(i);
      JSONObject instance = (JSONObject) c.ReadJson("/instances/" + uuid);
      Long index = (Long) instance.get("IndexInSeries");
      slices.add(new Slice((float) index, uuid));
    }

    return SortSlices(slices);
  }



  private String[] GetSlices(Dcm4cheeConnection c, 
                             JSONArray instances) throws IOException
  {
    String[] result;

    result = SortSlicesBy3D(c, instances);
    if (result != null && result.length == instances.size())
    {
      return result;
    }

    result = SortSlicesByNumber(c, instances);
    if (result != null && result.length == instances.size())
    {
      return result;
    }

    throw new IOException("Not a 3D image");
  }




  private ImagePlus image_;

  public DicomDecoder(final Dcm4cheeConnection c,
                      boolean isInstance,
                      String uuid) throws IOException, InterruptedException, ExecutionException
  {
    ImageStack stack = null;
    JSONObject tags = null;
    String tagsUri, name;

    if (isInstance)
    {
      name = "Instance " + uuid;
      tags = (JSONObject) c.ReadJson("/instances/" + uuid + "/tags");
      stack = AddSlice(stack, c, uuid);
    }
    else
    {
      name = "Series " + uuid;

      JSONObject series = (JSONObject) c.ReadJson("/series/" + uuid);
      JSONArray instances = (JSONArray) series.get("Instances");

      try
      {
        tags = (JSONObject) c.ReadJson("/series/" + uuid + "/shared-tags");
      }
      catch (Exception e)
      {
        // Fallback for old versions of Dcm4chee, without "shared-tags"
        tags = (JSONObject) c.ReadJson("/instances/" + (String) instances.get(0) + "/tags");
      }

      final String[] slices = GetSlices(c, instances);
      final ProgressDialog progress = new ProgressDialog(slices.length);

      try
      {
        progress.setVisible(true);
        SwingWorker<ImageStack, Float> worker = new SwingWorker<ImageStack, Float>() {
          @Override
          protected ImageStack doInBackground()
          {
            try
            {
              ImageStack stack = null;

              for (int i = 0; i < slices.length; i++)
              {
                if (progress.IsCanceled())
                {
                  return null;
                }

                progress.SetProgress(i);
                stack = AddSlice(stack, c, slices[i]);
              }

              return stack;
            }
            catch (IOException e)
            {
              return null;
            }
          }
        };

        worker.execute();
        stack = worker.get();
      }
      finally
      {
        progress.setVisible(false);
      }
    }

    image_ = new ImagePlus(name, stack);
    
    ExtractCalibration(image_, tags);
    ExtractPixelSpacing(image_, tags);
    ExtractDicomInfo(image_, tags);
  }

  public ImagePlus GetImage()
  {
    return image_;
  }
}
