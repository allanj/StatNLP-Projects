package com.statnlp.ui.visualize;


import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import org.apache.commons.collections15.Factory;

import com.statnlp.ui.ExpGlobal;
import com.statnlp.ui.MVCViewer;
import com.statnlp.ui.visualize.plugin.EditingModalGraphMouse2;
import com.statnlp.ui.visualize.plugin.MyEditingGraphMousePlugin;
import com.statnlp.ui.visualize.plugin.MyMouseMenus;
import com.statnlp.ui.visualize.plugin.MyPickingGraphMousePlugin;
import com.statnlp.ui.visualize.plugin.MyPopupVertexEdgeMenuMousePlugin;
import com.statnlp.ui.visualize.plugin.MyEditingGraphMousePlugin.EdgeChecker;
import com.statnlp.ui.visualize.plugin.MyEditingGraphMousePlugin.VertexChecker;
import com.statnlp.ui.visualize.type.VLink;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph.Itemtype;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.GraphMousePlugin;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTextField;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;

public class ExpDrawModelDialog extends JFrame implements ItemListener, MVCViewer {


	private JPanel contentPane;

	private JPanel panel_draw;

	ArrayList<JRadioButton> DrawTypeList = new ArrayList<JRadioButton>();
	
	ExpVisualizationEngine eve = new ExpVisualizationEngine(this);

	//ExpVisualizeGraph eg = new ExpVisualizeGraph();


	MyVertexChecker vCheck;
	MyEdgeChecker eCheck;

	Factory<VNode> vertexFactory;
	Factory<VLink> edgeFactory;
	private JTextField textField_bIndex;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ExpDrawModelDialog frame = new ExpDrawModelDialog();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ExpDrawModelDialog() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 620, 523);
		contentPane = new JPanel();
		contentPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				panel_draw.setBounds(100, 0, contentPane.getWidth() - 100, contentPane.getHeight());
				//System.out.println("contentPane bound = " + contentPane.getBounds());
				//System.out.println("drawPane bound = " + panel_draw.getBounds());
				
				eve.setSize(panel_draw.getWidth(), panel_draw.getHeight(), 10, 10);
			}
		});
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel panel_select = new JPanel();
		panel_select.setBounds(12, 5, 79, 267);
		contentPane.add(panel_select);

		JLabel lblSelect = new JLabel("Select type");
		lblSelect.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		panel_select.add(lblSelect);

		JRadioButton rdbtnRoot = new JRadioButton("Root");
		rdbtnRoot.setSelected(true);
		panel_select.add(rdbtnRoot);

		JRadioButton rdbtnA = new JRadioButton("A     ");
		panel_select.add(rdbtnA);

		JRadioButton rdbtnE = new JRadioButton("E     ");
		panel_select.add(rdbtnE);

		JRadioButton rdbtnT = new JRadioButton("T     ");
		panel_select.add(rdbtnT);

		JRadioButton rdbtnI = new JRadioButton("I      ");
		panel_select.add(rdbtnI);

		JRadioButton rdbtnX = new JRadioButton("X     ");
		panel_select.add(rdbtnX);

		JRadioButton rdbtnEdge = new JRadioButton("Edge");
		panel_select.add(rdbtnEdge);

		ButtonGroup group = new ButtonGroup();// 定义一个按钮组
		group.add(rdbtnX);
		group.add(rdbtnI);
		group.add(rdbtnT);
		group.add(rdbtnE);
		group.add(rdbtnA);
		group.add(rdbtnRoot);

		group.add(rdbtnEdge);

		DrawTypeList.clear();
		Enumeration<AbstractButton> abs = group.getElements();
		while (abs.hasMoreElements()) {
			JRadioButton jrbtn = (JRadioButton) abs.nextElement();
			jrbtn.addItemListener(this);
			DrawTypeList.add(jrbtn);
		}

		panel_draw = new JPanel();
		panel_draw.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				//ExpGlobal.VisualizationState.bIndex = Integer.parseInt(textField_bIndex.getText());
				//System.out.println("ExpGlobal.VisualizationState.bIndex = " + ExpGlobal.VisualizationState.bIndex);
			}
		});
		panel_draw.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		panel_draw.setBounds(100, 0, 517, 473);
		contentPane.add(panel_draw);
		
		JButton btnSetLocation = new JButton("Set Location");
		btnSetLocation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eve.setNodeLocation();
				eve.vv.repaint();
				
			}
		});
		btnSetLocation.setBounds(0, 403, 102, 29);
		contentPane.add(btnSetLocation);
		
		textField_bIndex = new JTextField();
		textField_bIndex.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				ExpGlobal.VisualizationState.bIndex = Integer.parseInt(textField_bIndex.getText().trim());
				System.out.println("ExpGlobal.VisualizationState.bIndex = " + ExpGlobal.VisualizationState.bIndex);
			}
		});
		textField_bIndex.setText("1");
		textField_bIndex.setBounds(57, 284, 45, 29);
		contentPane.add(textField_bIndex);
		textField_bIndex.setColumns(10);
		
		JLabel lblBindex = new JLabel("bIndex:");
		lblBindex.setBounds(6, 290, 61, 16);
		contentPane.add(lblBindex);
		
		JButton btnUpdateId = new JButton("Update ID");
		btnUpdateId.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eve.vg.updateAllNodeID();
			}
		});
		btnUpdateId.setBounds(0, 380, 102, 29);
		contentPane.add(btnUpdateId);
		
		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eve.vv.repaint();
			}
		});
		btnRefresh.setBounds(0, 431, 102, 29);
		contentPane.add(btnRefresh);

		InitGraph();
		InitVisualization();

	}

	public void itemStateChanged(ItemEvent e) {
		

		if (e.getStateChange() == ItemEvent.SELECTED) {
			Object obj = e.getSource();
			ExpGlobal.VisualizationState.Select_Item_Type = DrawTypeList.indexOf(obj);
			//System.out.println("Select Draw Type : " + plugin.select_type_id + " " + Itemtype.getType(plugin.select_type_id));
		}
	}

	void InitGraph() {

		vertexFactory = new Factory<VNode>() { // My vertex factory
			public VNode create() {
				VNode node = VNode.createNode();
				node.setType(ExpGlobal.VisualizationState.Select_Item_Type);
				if (node.type != Itemtype.X)
				{
					node.setbIndex(ExpGlobal.VisualizationState.bIndex);
				}
				eve.vg.addNode(node);
				return node;
			}
		};
		edgeFactory = new Factory<VLink>() { // My edge factory
			public VLink create() {
				
				VNode start = ExpGlobal.VisualizationState.linkStartNode;
				VNode end = ExpGlobal.VisualizationState.linkEndNode;
				VLink link = VLink.createEdge(start, end);
				
				if (end.type != Itemtype.X && start.isTypeParentOf(end))
				{
					//update bIndex, tag_length, tag_id(T node)
					end.setParent(start);
					
					start.update_bIndex_downwards();
					
					
					//if the EndNode is Tag node, update tag_length for all the related nodes
					if (end.type == Itemtype.T)
					{
						start.update_tag_length_upwards();
						start.update_tag_length_downwards();
					}
					else if (end.type == Itemtype.I)
					{
						start.update_tag_length_downwards();
					}
					else
					{
						end.update_tag_length_upwards();
					}
					
					//update_node_id for all the related nodes
					
				}
				ExpGlobal.VisualizationState.linkStartNode = null;
				ExpGlobal.VisualizationState.linkEndNode = null;
				return link;
			}
		};
		vCheck = new MyVertexChecker();
		eCheck = new MyEdgeChecker();

	}

	void InitVisualization() {

		eve.InitVisualization(panel_draw.getWidth(), panel_draw.getHeight(), 10, 10, "Static");
		
		final VisualizationViewer<VNode, VLink> vv = eve.vv;
		JFrame frame = this;
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.pack();
		// frame.setVisible(true);
		
		vv.addKeyListener(new KeyListener()
		{

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
				
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub
				//System.out.println(e.getKeyCode() + " is pressed for vv.  VK_DELETE=" + KeyEvent.VK_DELETE + "  " + KeyEvent.VK_BACK_SPACE);
				int KeyCode = e.getKeyCode();
				switch (KeyCode)
				{
				case KeyEvent.VK_DELETE:
				case KeyEvent.VK_BACK_SPACE:
					eve.removeNodes(vv.getPickedVertexState().getPicked());
					eve.removeEdges(vv.getPickedEdgeState().getPicked());
					break;
				case KeyEvent.VK_F5:
					eve.vv.repaint();
					break;
				}
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		
		


		// Create a graph mouse and add it to the visualization viewer
		// Our Vertices are going to be Integer objects so we need an Integer factory
		EditingModalGraphMouse2 gm = new EditingModalGraphMouse2(vv.getRenderContext(), this.vertexFactory, this.edgeFactory);
		vv.setGraphMouse(gm);
		panel_draw.add(vv);
		

		MyEditingGraphMousePlugin editPlugin = new MyEditingGraphMousePlugin(this.vertexFactory, this.edgeFactory);
		gm.remove(gm.getEditingPlugin());
		editPlugin.setVertexChecker(this.vCheck);
		editPlugin.setEdgeChecker(this.eCheck);
		gm.setEditingPlugin(editPlugin);
		

		
		MyPopupVertexEdgeMenuMousePlugin popupPlugin = new MyPopupVertexEdgeMenuMousePlugin(eve.vg);
	        // Add some popup menus for the edges and vertices to our mouse plugin.	 
	    JPopupMenu vertexMenu = new MyMouseMenus.VertexMenu(frame);
	    popupPlugin.setVertexPopup(vertexMenu);
	    JPopupMenu edgeMenu = new MyMouseMenus.EdgeMenu(frame);
	    popupPlugin.setEdgePopup(edgeMenu);
	    gm.remove(gm.getPopupEditingPlugin());  // Removes the existing popup editing plugin
	    gm.add(popupPlugin);   // Add our new plugin to the mouse
	    
	    MyPickingGraphMousePlugin pickPlugin = new MyPickingGraphMousePlugin(this.eve);
	    pickPlugin.setFrame(frame);
	    gm.setPickPlugin(pickPlugin);
	    

		// Let's add a menu for changing mouse modes
		JMenu modeMenu = gm.getModeMenu();
		modeMenu.setText("Edit Mode");
		modeMenu.setIcon(null); // I'm using this in a main menu
		modeMenu.setPreferredSize(new Dimension(80, 20)); // Change the size so I can see the text
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(modeMenu);
		frame.setJMenuBar(menuBar);
		
		gm.setMode(ModalGraphMouse.Mode.EDITING); // Start off in editing mode
		


	}

	public class MyVertexChecker implements VertexChecker<VNode, VLink> {

		public boolean checkVertex(Graph<VNode, VLink> g,
				VisualizationViewer<VNode, VLink> vv, VNode v) {
		
			// Will test to see if the graph has more that 5 vertices
			if (g.getVertexCount() < ExpGlobal.VisualizationState.MAX_NUM_NODE) {
				return true;
			} else {
				JOptionPane.showMessageDialog(vv,
						"UNKNOWN Vertex Error",
						"Vertex Check", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}

		@Override
		public boolean vertexMode() {
			// TODO Auto-generated method stub
			return (ExpGlobal.VisualizationState.Select_Item_Type < ExpGlobal.VisualizationState.SELECT_EDGE);
		}
	}

	public class MyEdgeChecker implements EdgeChecker<VNode, VLink> {

		public boolean checkEdge(Graph<VNode, VLink> g,
				VisualizationViewer<VNode, VLink> vv, VLink edge, VNode start,
				VNode end, EdgeType dir) {

			/*
			 * if (dir == EdgeType.DIRECTED) { JOptionPane.showMessageDialog(vv,
			 * "No Directed edges allowed Today!", "Edge Check",
			 * JOptionPane.ERROR_MESSAGE); return false; } else
			 */
			if (start.equals(end))
			{
				JOptionPane.showMessageDialog(vv,
						"No self-directed edges allowed in this graph!",
						"Edge Check", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			
			if (g.findEdge(start, end) != null) {
				JOptionPane.showMessageDialog(vv,
						"No parallel edges allowed in this graph!",
						"Edge Check", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (edge == null) {
				System.err.println("Edge is null in ExpDrawModel.java");
				return false;
			}

			return true;

		}

		@Override
		public boolean edgeMode() {
			// TODO Auto-generated method stub
			return (ExpGlobal.VisualizationState.Select_Item_Type == ExpGlobal.VisualizationState.SELECT_EDGE);
		}

		@Override
		public boolean setEnddingNodes(VNode start, VNode end) {
			// TODO Auto-generated method stub
            ExpGlobal.VisualizationState.linkStartNode =  start;
            ExpGlobal.VisualizationState.linkEndNode =  end;
            
			return true;
		}

	}

	@Override
	public void updateMVCViewer(String arg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endOfExperiment() {
		// TODO Auto-generated method stub
		
	}

}
