package com.statnlp.ui.visualize;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import com.statnlp.commons.types.InputToken;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.ui.ExpGlobal;
import com.statnlp.ui.visualize.type.VLink;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public abstract class VisualizationViewerEngine {

	public static boolean DEBUG = true & ExpGlobal.DEBUG;
	
	static int layout_width = 1024;

	static int layout_height = 1024;
	
	static int margin_width = 50;
	
	static int margin_height = 50;

	static double span_width = 50;

	static double span_height = 50.0;
	
	static double offset_width = 100;
	
	static double offset_height = 100;
	
	
	
	
	
	protected Layout<VNode, VLink> layout;
	
	protected VisualizationViewer<VNode, VLink> vv;
	
	protected VisualizeGraph vg = new VisualizeGraph();
	
	//node_id to label
	protected HashMap<Long, String> labelMap = new HashMap<Long, String>();
	
	//node_type/node_id to color
	protected Color[] colorMap = null;
	
	//node_id to coordinate
	protected HashMap<Long, Point2D> coordinateMap = new HashMap<Long, Point2D>();
	
	
	protected TableLookupNetwork network;
	
	protected InputToken[] input_tokens;

	public VisualizationViewerEngine(int TypeLength) {
		colorMap = new Color[TypeLength];
		initTypeColorMapping();
	}
	
	protected void initTypeColorMapping()
	{
		for(int i = 0; i < colorMap.length; i++)
		{
			colorMap[i] = VLink.commoncolor[i % VLink.commoncolor.length];
		}
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null)
		for(VNode node : vg.getNodes())
		{
			node.typeID = node.ids[4];
			node.color = colorMap[node.typeID];
		}
		
	}
	
	protected double x_mapping(double x)
	{
		return x;
	}
	
	protected double y_mapping(double y)
	{
		return y;
	}
	
	protected abstract void initNodeCoordinate(VisualizeGraph vg);
	
	protected abstract String label_mapping(int[] ids);
	
	
	protected void initNodeLabel(VisualizeGraph vg)
	{
		
		for(VNode node : vg.getNodes())
		{
			node.label = label_mapping(node.ids);		
		}
		
	}
	
	
	public void visualizeNetwork(TableLookupNetwork network, JFrame frame, String title)
	{
		this.network = network;
		
		long nodes_arr[] = network.getAllNodes();
		int childrens_arr[][][] = network.getAllChildren();
		
		vg.clear();
		vg.buildArrayToGraph(nodes_arr, childrens_arr);
		
		initNodeColor(vg);
		initNodeLabel(vg);
	
		this.InitVisualization(layout_width, layout_height, margin_width, margin_height, "static");
		
		initNodeCoordinate(vg);
		
		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		// gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON1_MASK));
		gm.add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, 1.1f, 0.9f));
		vv.setGraphMouse(gm);
		
		if (frame == null)
			frame = new JFrame(title);
		
		final JFrame theframe = frame;
		final String imageTitle = title;
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		
		
//		Dimension d = theframe.getContentPane().getSize();
//		saveImage(imageTitle, d);
		
		frame.setVisible(true);
	}


	
	protected void setSize(int layout_width, int layout_height, int margin_width, int margin_height)
	{
		layout.setSize(new Dimension(layout_width, layout_height)); // sets the initial size of the layout space
		
		vv.setPreferredSize(new Dimension(layout_width - margin_width, layout_height - margin_height)); // Sets the viewing area size
	}
	
	protected void InitVisualization(int layout_width, int layout_height, int margin_width, int margin_height, String layout_type)
	{
		if (layout_type.equals("FR"))
		{
			layout = new FRLayout<VNode, VLink>(vg.g);
		}
		else if (layout_type.equals("Spring"))
		{
			layout = new SpringLayout<VNode, VLink>(vg.g);
		}
		else
		{
			layout = new StaticLayout<VNode, VLink>(vg.g);
		}
		
		vv = new VisualizationViewer<VNode, VLink>(layout);
		
		this.setSize(layout_width, layout_height, margin_width, margin_height);
		vv.setBackground(Color.WHITE);

		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderContext().setVertexLabelTransformer(vertexLabel);
		
		
		
		vv.getRenderContext().setEdgeFillPaintTransformer(edgePaint);
		// vv.getRenderContext().setEdgeLabelTransformer(edgeLabel);
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStroke);
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}

	public Transformer<VNode, String> vertexLabel = new Transformer<VNode, String>() {
		public String transform(VNode node) {
			return node.label;
		}
	};
	

	public Transformer<VNode, Paint> vertexPaint = new Transformer<VNode, Paint>() {
		public Paint transform(VNode node) {
			return node.color;
		}
	};


	
	

	public static Transformer<VLink, Paint> edgePaint = new Transformer<VLink, Paint>() {
		public Paint transform(VLink i) {
			return VLink.commoncolor[i.hyperlink.id % VLink.commoncolor.length];
		}
	};

	public static Transformer<VLink, String> edgeLabel = new Transformer<VLink, String>() {
		public String transform(VLink i) {
			return "Hyper ID: " + i.getHyperID();
		}
	};

	public static Transformer<VLink, Stroke> edgeStroke = new Transformer<VLink, Stroke>() {
		float dash[] = { 10.0f };

		public Stroke transform(VLink i) {
			return new BasicStroke(0.8f);
		}
	};
	
	
	
	
	
	

}
