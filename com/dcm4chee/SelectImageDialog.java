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

import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Enumeration;


import java.awt.Canvas;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.xml.bind.DatatypeConverter;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;


public class SelectImageDialog extends JDialog 
{
	private enum ResourceType {
		ROOT,
      SERVER,
			PATIENT,
			STUDY,
			SERIES,
			INSTANCE,
      INFO;
	}


	private static class MyTreeNode extends DefaultMutableTreeNode implements Comparable 
  {
    private Dcm4cheeConnection Dcm4chee_;
		private boolean loaded_ = false;
		private ResourceType type_;
		private String uuid_;


    public String GetUuid()
    {
      return uuid_;
    }

    public ResourceType GetResourceType()
    {
      return type_;
    }

    public Dcm4cheeConnection GetConnection()
    {
      return Dcm4chee_;
    }

    public boolean UpdatePreview(PreviewPanel preview)
    {
      if (type_ == ResourceType.INSTANCE)
      {
        preview.Load(Dcm4chee_, "/instances/" + uuid_ + "/preview");
        return true;
      }
      else if (type_ == ResourceType.SERIES)
      {
        try
        {
          JSONObject series = (JSONObject) Dcm4chee_.ReadJson("series/" + uuid_);
          JSONArray instances = (JSONArray) series.get("Instances");
          if (instances.size() > 0)
          {
            preview.Load(Dcm4chee_, "/instances/" + instances.get(0) + "/preview");
            return true;
          }
        }
        catch (IOException e)
        {
        }
      }

      preview.Reset();
      return false;
    }


    private String AddComponent(String source, String component)
    {
      if (component == null ||
          component.length() == 0)
      {
        return source;
      }

      if (source.length() == 0)
      {
        return component;
      }
      else
      {
        return source + " - " + component;
      }
    }


    private List<MyTreeNode> LoadPatients() throws IOException
    {
      List<MyTreeNode> children = new ArrayList<MyTreeNode>();

      JSONArray patients = (JSONArray) Dcm4chee_.ReadJson("patients");
      for (int i = 0; i < patients.size(); i++)
      {
        String uuid = (String) patients.get(i);
        JSONObject patient = (JSONObject) Dcm4chee_.ReadJson("patients/" + uuid);
        JSONObject main = (JSONObject) patient.get("MainDicomTags");

        String s = new String();
        s = AddComponent(s, (String) main.get("PatientID"));
        s = AddComponent(s, (String) main.get("PatientName"));
        children.add(new MyTreeNode(Dcm4chee_, ResourceType.PATIENT, uuid, s));
      }      

      return children;
    }


    private List<MyTreeNode> LoadStudies() throws IOException
    {
      List<MyTreeNode> children = new ArrayList<MyTreeNode>();

      JSONObject patient = (JSONObject) Dcm4chee_.ReadJson("patients/" + uuid_);
      JSONArray studies = (JSONArray) patient.get("Studies");
      for (int i = 0; i < studies.size(); i++)
      {
        String uuid = (String) studies.get(i);
        JSONObject study = (JSONObject) Dcm4chee_.ReadJson("studies/" + uuid);
        JSONObject main = (JSONObject) study.get("MainDicomTags");

        String s = new String();
        s = AddComponent(s, (String) main.get("StudyDescription"));
        s = AddComponent(s, (String) main.get("StudyDate"));
        children.add(new MyTreeNode(Dcm4chee_, ResourceType.STUDY, uuid, s));
      }

      return children;
    }


    private List<MyTreeNode> LoadSeries() throws IOException
    {
      List<MyTreeNode> children = new ArrayList<MyTreeNode>();

      JSONObject study = (JSONObject) Dcm4chee_.ReadJson("studies/" + uuid_);
      JSONArray seriesSet = (JSONArray) study.get("Series");
      for (int i = 0; i < seriesSet.size(); i++)
      {
        String uuid = (String) seriesSet.get(i);
        JSONObject series = (JSONObject) Dcm4chee_.ReadJson("series/" + uuid);
        JSONObject main = (JSONObject) series.get("MainDicomTags");

        String s = new String();
        s = AddComponent(s, (String) main.get("Modality"));
        s = AddComponent(s, (String) main.get("SeriesDescription"));
        children.add(new MyTreeNode(Dcm4chee_, ResourceType.SERIES, uuid, s));
      }

      return children;
    }


    private List<MyTreeNode> LoadInstances() throws IOException
    {
      List<MyTreeNode> children = new ArrayList<MyTreeNode>();

      JSONObject series = (JSONObject) Dcm4chee_.ReadJson("series/" + uuid_);
      JSONArray instances = (JSONArray) series.get("Instances");
      for (int i = 0; i < instances.size(); i++)
      {
        String uuid = (String) instances.get(i);
        JSONObject instance = (JSONObject) Dcm4chee_.ReadJson("instances/" + uuid);
        Long index = (Long) instance.get("IndexInSeries");
        String s;
        if (index == null)
        {
          s = uuid;
        }
        else
        {
          s = String.valueOf(index);
        }

        children.add(new MyTreeNode(Dcm4chee_, ResourceType.INSTANCE, uuid, s));
      }

      return children;
    }


    public MyTreeNode()  // Create root node
    {
      loaded_ = true;
      Dcm4chee_ = null;
      type_ = ResourceType.ROOT;
      uuid_ = "";
			setAllowsChildren(true);
    }

		public MyTreeNode(Dcm4cheeConnection Dcm4chee,
                      ResourceType type, 
                      String id, 
                      String name) 
		{
      Dcm4chee_ = Dcm4chee;
			type_ = type;
			uuid_ = id;
			add(new DefaultMutableTreeNode("Loading...", false));
			setAllowsChildren(true);
			setUserObject(name);
		}

		@Override
    public int compareTo(Object other) 
		{
			String a = (String) getUserObject();
			String b = (String) ((MyTreeNode) other).getUserObject();
			return a.compareTo(b);
    }

		private void SetChildren(List<MyTreeNode> children) 
		{
      Collections.sort(children);

			removeAllChildren();
			setAllowsChildren(children.size() > 0);
			for (MutableTreeNode node : children) 
			{
				add(node);
			}
			loaded_ = true;
		}

		@Override
		public boolean isLeaf() 
		{
			return type_ == ResourceType.INSTANCE || type_ == ResourceType.INFO;
		}

		public void LoadChildren(final DefaultTreeModel model) 
		{
			if (loaded_) 
			{
				return;
			}

			SwingWorker<List<MyTreeNode>, Void> worker = new SwingWorker<List<MyTreeNode>, Void>() {
				@Override
				protected List<MyTreeNode> doInBackground() 
        {
					List<MyTreeNode> children = null;

          try
          {
            switch (type_)
            {
              case SERVER:   children = LoadPatients(); break;
              case PATIENT:  children = LoadStudies(); break;
              case STUDY:    children = LoadSeries(); break;
              case SERIES:   children = LoadInstances(); break;
              default:
                break;
            }            
          }
          catch (IOException e)
          {
            children = null;
          }

          if (children == null)
          {
            children = new ArrayList<MyTreeNode>();
            children.add(new MyTreeNode(Dcm4chee_, ResourceType.INFO, "", "Dcm4chee is not running, or bad credentials"));
          }

					return children;
				}

				@Override
				protected void done() 
        {
					try 
          {
						SetChildren(get());
						model.nodeStructureChanged(MyTreeNode.this);
					}
          catch (Exception e) 
          {
					}

					super.done();
				}
			};

			worker.execute();
		}

	}


  private MyTreeNode root_ = new MyTreeNode();
  private JTree tree_ = null;
  private boolean isSuccess_;
  private String selectedUuid_;
  private ResourceType selectedType_;
  private Dcm4cheeConnection selectedConnection_;
  private JButton okButton_ = new JButton("Open");
  private JButton removeServer_ = new JButton("Remove server");
  private PreviewPanel preview_ = new PreviewPanel();

  public SelectImageDialog()
  {
    tree_ = new JTree();

    tree_.addTreeWillExpandListener(new TreeWillExpandListener() 
    {
      @Override
      public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();
        if (path.getLastPathComponent() instanceof MyTreeNode) {
          MyTreeNode node = (MyTreeNode) path.getLastPathComponent();
          node.LoadChildren((DefaultTreeModel) tree_.getModel());
        }
      }

      @Override
      public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      }
    });


    tree_.addTreeSelectionListener(new TreeSelectionListener()
    {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        TreePath path = e.getNewLeadSelectionPath();
        if (path != null) 
        {
          MyTreeNode node = (MyTreeNode) path.getLastPathComponent();
          if (node.UpdatePreview(preview_))
          {
            selectedType_ = node.GetResourceType();
            selectedUuid_ = node.GetUuid();
            selectedConnection_ = node.GetConnection();
            okButton_.setEnabled(true);
          }

          removeServer_.setEnabled(node.GetResourceType() == ResourceType.SERVER);
        }
      }
    });


    tree_.addMouseListener(new MouseAdapter() 
    {
      public void mousePressed(MouseEvent e) {
        TreePath path = tree_.getPathForLocation(e.getX(), e.getY());
        if (path != null) {
          MyTreeNode node = (MyTreeNode) path.getLastPathComponent();
          if (e.getClickCount() == 2 &&
              node.GetResourceType() == ResourceType.INSTANCE) {
            // Double click on an instance, close the dialog
            isSuccess_ = true;
            setVisible(false);
          }
        }
      }
    });


    final JPanel contentPanel = new JPanel();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JSplitPane splitPane = new JSplitPane();
			splitPane.setResizeWeight(0.6);
			contentPanel.add(splitPane);
			
			splitPane.setLeftComponent(new JScrollPane(tree_));
			splitPane.setRightComponent(preview_);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton btnAddServer = new JButton("Add server");
        btnAddServer.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            Dcm4cheeConfigurationDialog dd = new Dcm4cheeConfigurationDialog();
            dd.setLocationRelativeTo(null);  // Center dialog on screen

            Dcm4cheeConnection Dcm4chee = dd.ShowModal();
            if (Dcm4chee != null) {
              AddDcm4chee(Dcm4chee);
              ((DefaultTreeModel) tree_.getModel()).reload();
            }
          }
        });
				buttonPane.add(btnAddServer);
			}

			{
				buttonPane.add(removeServer_);
        removeServer_.setEnabled(false);

        removeServer_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            MyTreeNode selected = (MyTreeNode) tree_.getLastSelectedPathComponent(); 
            if (selected.GetResourceType() == ResourceType.SERVER &&
                JOptionPane.showConfirmDialog(null, "Remove server \"" + selected.getUserObject() + "\"?",
                                              "WARNING", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
              ((DefaultTreeModel) tree_.getModel()).removeNodeFromParent(selected);
            }
          }
        });
			}

			{
        okButton_.setEnabled(false);
				okButton_.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            isSuccess_ = true;
            setVisible(false);
          }
        });
				buttonPane.add(okButton_);
				getRootPane().setDefaultButton(okButton_);
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
    setTitle("Select some series or some instance in Dcm4chee");
    setModal(true);
  }


  public void AddDcm4chee(Dcm4cheeConnection Dcm4chee)
  {
    root_.add(new MyTreeNode(Dcm4chee, ResourceType.SERVER, "", Dcm4chee.GetName()));
  }

  public boolean ShowModal()
  {
    final DefaultTreeModel model = new DefaultTreeModel(root_);
    root_.LoadChildren(model);
    tree_.setRootVisible(false);
    tree_.setShowsRootHandles(true);
    tree_.setModel(model);

    isSuccess_ = false;
    setVisible(true);
    return isSuccess_;
  }

  public String GetSelectedUuid()
  {
    return selectedUuid_;
  }

  public boolean IsSeriesSelected()
  {
    return selectedType_ == ResourceType.SERIES;
  }

  public boolean IsInstanceSelected()
  {
    return selectedType_ == ResourceType.INSTANCE;
  }

  public Dcm4cheeConnection GetSelectedConnection()
  {
    return selectedConnection_;
  }

  public void Select(Dcm4cheeConnection c, boolean isInstance, String uuid)  // For test
  {
    selectedConnection_ = c;
    selectedType_ = (isInstance ? ResourceType.INSTANCE : ResourceType.SERIES);
    selectedUuid_ = uuid;
  }

  public void Unserialize(String s)
  {
    if (s.length() == 0)
    {
      // Add default Dcm4chee server
      AddDcm4chee(new Dcm4cheeConnection());
    }
    else
    {
      String decoded = new String(DatatypeConverter.parseBase64Binary(s));
      JSONArray config = (JSONArray) JSONValue.parse(decoded);
      if (config != null)
      {
        for (int i = 0; i < config.size(); i++)
        {
          AddDcm4chee(Dcm4cheeConnection.Unserialize((JSONObject) config.get(i)));
        }
      }
    }
  }

  public String Serialize()
  {
    JSONArray servers = new JSONArray();

    for (int i = 0; i < root_.getChildCount(); i++)
    {
      MyTreeNode node = (MyTreeNode) root_.getChildAt(i);
      servers.add(node.GetConnection().Serialize());
    }

    String config = servers.toJSONString();
    return DatatypeConverter.printBase64Binary(config.getBytes());
  }
}
