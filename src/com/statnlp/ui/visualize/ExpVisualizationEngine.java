package com.statnlp.ui.visualize;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import com.statnlp.commons.AttributedWord;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.LocalNetworkLearnerThread;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.ie.io.EventExtractionReader;
import com.statnlp.ie.linear.IELinearConfig;
import com.statnlp.ie.linear.IELinearFeatureManager;
import com.statnlp.ie.linear.IELinearFeatureManager_GENIA;
import com.statnlp.ie.linear.IELinearInstance;
import com.statnlp.ie.linear.IELinearNetwork;
import com.statnlp.ie.linear.IELinearNetworkCompiler;
import com.statnlp.ie.linear.MentionExtractionLearner;
import com.statnlp.ie.linear.MentionLinearInstance;
import com.statnlp.ie.types.IEManager;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.Mention;
import com.statnlp.ie.types.MentionTemplate;
import com.statnlp.ie.types.SemanticTag;
import com.statnlp.ui.ExpEngine;
import com.statnlp.ui.ExpGlobal;
import com.statnlp.ui.ExpTrainingDataEngine;
import com.statnlp.ui.MVCModel;
import com.statnlp.ui.MVCViewer;
import com.statnlp.ui.visualize.type.HyperLink;
import com.statnlp.ui.visualize.type.VLink;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;
import com.statnlp.ui.visualize.type.VisualizeGraph.Itemtype;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.samples.SimpleGraphDraw;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class ExpVisualizationEngine implements MVCModel {

	public static boolean DEBUG = true & ExpGlobal.DEBUG;

	MVCViewer viewer = null;

	ExpEngine parent = null;

	String Status;
	
	public VisualizeGraph vg = new VisualizeGraph();
	
	public Layout<VNode, VLink> layout;
	
	public VisualizationViewer<VNode, VLink> vv;

	static int layout_width = 1024;

	static int layout_height = 1024;

	static double span_width = layout_width / 5.0;

	static double span_height = 100.0;
	



	public ExpVisualizationEngine(ExpEngine parent) {
		this.parent = parent;
	}
	
	public ExpVisualizationEngine(MVCViewer viewer) {
		this.setMVCViewer(viewer);
	}	
	
	

	@Override
	public void setMVCViewer(MVCViewer viewer) {
		this.viewer = viewer;
	}
	
	ArrayList<String >words = null;
	
	ArrayList<Mention> mentions = null;
			
	public void visualizeIELinearNetwork(int network_index)
	{
		ExpTrainingDataEngine etde = parent.getExpTrainingDataEngine();
		
		IELinearNetwork network = (IELinearNetwork)etde.getNetwork(network_index);
		
		LabeledTextSpan lts = etde.getLabeledTextSpan(network_index);
		
		AttributedWord[] attributedwords = lts._words;
		
		words = new ArrayList<String>();

		System.out.println("\nThere are " + attributedwords.length
				+ " Attributed Words.");
		for (AttributedWord word : attributedwords) {
			String w = "";
			for (String s : word.getAttribute("WORD")) {
				w = w + s;
			}
			words.add(w);
		}

		System.out.println();

		mentions = lts.getAllMentions();
		System.out.println("There are " + mentions.size()
				+ " mention types");
		for (Mention mention : mentions) {
			System.out.println(mention);
		}
		
		
		visualizeIELinearNetwork(network, network_index, words);
	}
	
	

	void visualizeIELinearNetwork(IELinearNetwork network, int network_index, ArrayList<String> words) {
		
		System.out.println("\nThere are " + network.countNodes()
				+ " nodes in network " + network_index + ".");


		vg.clear();
		
		long nodes_arr[] = network.getAllNodes();

		int childrens_arr[][][] = network.getAllChildren();
		
		vg.setArray(nodes_arr, childrens_arr);
		
		vg.setWords(words);
	
		vg.buildArrayToGraph();
	
		this.InitVisualization(ExpVisualizationEngine.layout_width, ExpVisualizationEngine.layout_height, 50, 50, "Spring");
		
		this.setNodeLocation();
	

		DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
		// gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
		gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON1_MASK));
		gm.add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, 1.1f, 0.9f));
		vv.setGraphMouse(gm);

		JFrame frame = new JFrame("Network " + network_index);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true);

	}
	
	public void setSize(int layout_width, int layout_height, int margin_width, int margin_height)
	{
		layout.setSize(new Dimension(layout_width, layout_height)); // sets the initial size of the layout space
		
		vv.setPreferredSize(new Dimension(layout_width - margin_width, layout_height - margin_height)); // Sets the viewing area size
	}
	
	public void InitVisualization(int layout_width, int layout_height, int margin_width, int margin_height, String layout_type)
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


		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderContext().setEdgeFillPaintTransformer(edgePaint);

		vv.getRenderContext().setVertexLabelTransformer(vertexLabel);
		// vv.getRenderContext().setEdgeLabelTransformer(edgeLabel);

		vv.getRenderContext().setEdgeStrokeTransformer(edgeStroke);

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	}

	@Override
	public String getStatus() {
		return Status;
	}

	@Override
	public void setStatus(String status) {
		this.Status = status;
	}

	void setChildLocation(ArrayList<VNode> last_level, int bIndex, int depth)  {
		
		/*if (depth >= 7)
		{
			System.err.println("Warning: There is a loop in the graph model.");
			return;
		}*/

		ArrayList<VNode> children = new ArrayList<VNode>();
		
		/*
		if (bIndex == 5) {
			if (DEBUG)
				System.out.println("Depth " + depth + ": " + last_level);
		}*/

		for (VNode node : last_level) {
			for (HyperLink hyperlink : node.hyperlinks) {
				for (VLink link : hyperlink.links) {

					VNode child = link.Dest;

					if (bIndex == child.bIndex && bIndex != 0 && node.type != child.type) {
						children.add(child);
					}
					
					
					if (bIndex != 0 && child.type == Itemtype.I)
					{
						children.add(child);
					}
				}

			}
		}

		int i = 0;
		double sub_span = span_width / (children.size() + 1.0);

		for (VNode child : children) {
			child.point = new Point2D.Double(
					bIndex * span_width + i * sub_span, span_height * depth);

			layout.setLocation(child, child.point);
			layout.lock(child, true);
			i++;

		}

	

		if (children.isEmpty()) {
			return;
		}

		setChildLocation(children, bIndex, depth + 1);

	}

	//Layout<VNode, VLink> layout, ArrayList<VNode> nodes
	public void setNodeLocation() {
		System.out.print("Set Node Location: ");

		ArrayList<VNode> roots = new ArrayList<VNode>();
		
		ArrayList<VNode> nodes = vg.getNodes();

		for (int k = 0; k < nodes.size(); k++) {
			VNode node = nodes.get(k);

			if (node.type == Itemtype.ROOT) // node.indegree == 0
			{
				System.out.print(node.label + "|");

				node.point = new Point2D.Double(node.bIndex * span_width, 100);
				
				layout.setLocation(node, node.point);
				layout.lock(node, true);
				roots.add(node);

			}

			if (node.type == Itemtype.X) // node_array[0] == 0
			{
				node.point = new Point2D.Double(span_width, 700);
				layout.setLocation(node, node.point);
				layout.lock(node, true);
			}

		}
		System.out.println();
		// System.out.println(" Size=" + root_children_list.size());

		for (int i = 0; i < roots.size(); i++) {
			VNode node = roots.get(i);
			ArrayList<VNode> last_level = new ArrayList<VNode>();
			last_level.add(node);

			setChildLocation(last_level, node.bIndex, 2);
		}

	}
	
	public void markPickedNodes()
	{
		PickedState<VNode> pickedVertexState = vv.getPickedVertexState();
		
		for(VNode node : vg.getNodes())
		{
			node.Pick(false);
		}
		
		for(VNode node : pickedVertexState.getPicked())
		{
			node.Pick(true);
		}
	}
	
	public void removeNode(VNode node)
	{
		this.vg.removeNode(node);
		this.vv.repaint();
	}
	
	public void removeNodes(Set<VNode> nodes)
	{
		this.vg.removeNodes(nodes);
		this.vv.repaint();
	}
	
	public void removeEdge(VLink link)
	{
		this.vg.removeEdge(link);
		this.vv.repaint();
	}
	
	public void removeEdges(Set<VLink> edges)
	{
		this.vg.removeEdges(edges);
		this.vv.repaint();
	}
	
	public IELinearNetwork toIELineaerNetwork(int networkId, IELinearInstance inst, LocalNetworkParam param)
	{
		long[] nodes = this.vg.getNodesArray();
		int[][][] children = this.vg.getChildrensArray();
		int numNodes = this.vg.getNodes().size();
		return new IELinearNetwork(networkId, inst, nodes, children, param, numNodes);
	}
	
	public IELinearNetwork toIELineaerNetwork(int networkId, int inst_id, LocalNetworkParam param)
	{
		MentionLinearInstance inst = this.toMentionLinearInstance(inst_id);
		return toIELineaerNetwork(networkId, inst, param);
	}
	
	public MentionLinearInstance toMentionLinearInstance(int inst_id)
	{
		IEManager manager = null;
		LabeledTextSpan output = this.getLabeledTextSpan(manager);
		
		
		
		MentionTemplate info = null;

		return new MentionLinearInstance(inst_id, output, info);
	}
	
	
	
	public LabeledTextSpan getLabeledTextSpan(IEManager manager)
	{
		String[] words = this.vg.getWords();
		String[] tags = this.vg.getTags();
		String[] annotations = null;///
		
		AttributedWord[] aws = new AttributedWord[words.length];
		for(int k = 0; k<words.length; k++){
			aws[k] = new AttributedWord(words[k]);
			
			//else if(!_corpusName.equals("GENIA")){
				aws[k].addAttribute("POS", tags[k]);
			//}
			
		}
		
		LabeledTextSpan span = new LabeledTextSpan(aws);
		for(int k = 0; k<annotations.length; k++){
			String[] annotation = annotations[k].split("\\s");
			String[] indices = annotation[0].split(",");
			int bIndex, eIndex, head_bIndex, head_eIndex;
			if(indices.length == 2){
				bIndex = Integer.parseInt(indices[0]);
				eIndex = Integer.parseInt(indices[1]);
				head_bIndex = Integer.parseInt(indices[0]);
				head_eIndex = Integer.parseInt(indices[1]);
			} else if(indices.length == 4){
				bIndex = Integer.parseInt(indices[0]);
				eIndex = Integer.parseInt(indices[1]);
				head_bIndex = Integer.parseInt(indices[2]);
				head_eIndex = Integer.parseInt(indices[3]);
			} else {
				throw new RuntimeException("The number of indices is "+indices.length);
			}
			String label = annotation[1];
			span.label(bIndex, eIndex, new Mention(bIndex, eIndex, head_bIndex, head_eIndex, manager.toMentionType(label)));
		}
		
		return span;
		
	}
	
	

	public static Transformer<VNode, Paint> vertexPaint = new Transformer<VNode, Paint>() {
	
		public Paint transform(VNode i) {
		
			return i.getColor();
		}
				
	};

	public static Transformer<VNode, String> vertexLabel = new Transformer<VNode, String>() {
		public String transform(VNode i) {
			return i.label;
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
