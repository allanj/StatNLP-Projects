package com.statnlp.projects.entity.semi;

import java.awt.Color;
import java.awt.geom.Point2D;

import com.statnlp.projects.entity.lcr2d.E2DNetworkCompiler.NODE_TYPES;
import com.statnlp.ui.visualize.VisualizationViewerEngine;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

public class SemiViewer extends VisualizationViewerEngine {



	
	
	public SemiViewer() {
		super(3);
	}

	protected void initTypeColorMapping()
	{	
		colorMap[0] = Color.GREEN;  //leaf and root
		colorMap[1] = Color.WHITE;  //entity
		colorMap[2] = Color.YELLOW; //O
		
	}
	
	
	@Override
	protected String label_mapping(int[] ids) {
		
		int pos = ids[0];
		int labelid = ids[2];
		String type = "Root";
		if(labelid<Label.LABELS.size())
			type = Label.get(labelid).getForm();
		String label = pos+","+type;
		return label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null)
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			
			int color_index = -1;
			if(ids[1]==NODE_TYPES.LEAF.ordinal() || ids[1]==NODE_TYPES.ROOT.ordinal())
				color_index = 0;
			else 
				color_index = 1;
			node.color = colorMap[color_index];
		}
		
	}
	
	protected void initNodeCoordinate(VisualizeGraph vg)
	{
		
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			
			int pos = ids[0];
			int tagId = ids[2];
			
			double x = pos;
			double y = tagId;
			if(ids[1]==NODE_TYPES.LEAF.ordinal()){
				x-=3;
				y+=5;
			}
			
			if(ids[1]==NODE_TYPES.ROOT.ordinal()){
				x+=0.5;
			}
				
			
			
			//System.err.println(Arrays.toString(ids)+" coordinate:"+x+","+y);
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);

		}
	}

}
