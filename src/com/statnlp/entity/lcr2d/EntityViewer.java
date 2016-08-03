package com.statnlp.entity.lcr2d;

import java.awt.Color;
import java.awt.geom.Point2D;

import com.statnlp.entity.lcr2d.E2DNetworkCompiler.NODE_TYPES;
import com.statnlp.ui.visualize.VisualizationViewerEngine;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

public class EntityViewer extends VisualizationViewerEngine {


	
	private String[] types;
	
	public EntityViewer() {
		super(6);
	}
	
	public EntityViewer( String[] types) {
		super(6);
		this.types = types;
	}

	protected void initTypeColorMapping()
	{	
		colorMap[0] = Color.GREEN;  //leaf and root
		colorMap[1] = Color.WHITE;  //entity
		colorMap[2] = Color.RED;  //entity
		colorMap[3] = Color.GRAY;  //entity
		colorMap[4] = Color.PINK;  //entity
		colorMap[5] = Color.YELLOW; //O
		
	}
	
	
	@Override
	protected String label_mapping(int[] ids) {
		
		int pos = ids[0];
		int tagid = ids[2];
		String dir = ids[1]==1? "RA": "LA";
		String type = types[tagid];
		String label = pos+","+type+","+dir;
		return label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null)
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			
			int color_index = -1;
			if(ids[4]==NODE_TYPES.LEAF.ordinal() || ids[4]==NODE_TYPES.ROOT.ordinal())
				color_index = 0;
			else 
				color_index = ids[2]+1;
			node.color = colorMap[color_index];
		}
		
	}
	
	protected void initNodeCoordinate(VisualizeGraph vg)
	{
		
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			
			int pos = ids[0];
			int direction = ids[1];
			int tagId = ids[2];
			
			
			double x = pos+direction*1.0/2;
			double y = tagId;
			
			
			//System.err.println(Arrays.toString(ids)+" coordinate:"+x+","+y);
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);

		}
	}

}
