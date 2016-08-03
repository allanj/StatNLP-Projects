package com.statnlp.entity.dcrf;

import java.awt.Color;
import java.awt.geom.Point2D;

import com.statnlp.entity.dcrf.DCRFNetworkCompiler.NODE_TYPES;
import com.statnlp.ui.visualize.VisualizationViewerEngine;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

public class DCRFViewer extends VisualizationViewerEngine {


	
	
	public DCRFViewer() {
		super(4);
	}
	

	protected void initTypeColorMapping()
	{	
		colorMap[0] = Color.WHITE;  //leaf
		colorMap[1] = Color.GREEN;  //entity
		colorMap[2] = Color.PINK;  //pos
		colorMap[3] = Color.YELLOW; //root
		
	}
	
	
	@Override
	protected String label_mapping(int[] ids) {
		int pos = ids[0];
		int labelId = ids[1];
		
		String type = "";
		if(ids[4]==NODE_TYPES.entLEAF.ordinal()){
			type = "entity leaf";
		}else if (ids[4]==NODE_TYPES.tagLEAF.ordinal()){
			type = "tag leaf";
		}else if (ids[4]==NODE_TYPES.entNODE.ordinal()){
			type = Entity.ENTS_INDEX.get(labelId).getForm();
		}else if (ids[4]==NODE_TYPES.tagNODE.ordinal()){
			type = Tag.TAGS_INDEX.get(labelId).getForm();
		}else if (ids[4]==NODE_TYPES.ROOT.ordinal()){
			type = "root node";
		}
		String label = pos+","+type;
		return label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null)
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			//int rightIndex = ids[0];
			//int leftIndex = ids[0] - ids[1];
			int color_index = -1;
			if(ids[4]==NODE_TYPES.entLEAF.ordinal()){
				color_index = 0;
			}else if (ids[4]==NODE_TYPES.tagLEAF.ordinal()){
				color_index = 0;
			}else if (ids[4]==NODE_TYPES.entNODE.ordinal()){
				color_index = 1;
			}else if (ids[4]==NODE_TYPES.tagNODE.ordinal()){
				color_index = 2;
			}else if (ids[4]==NODE_TYPES.ROOT.ordinal()){
				color_index = 3;
			}
			
			node.color = colorMap[color_index];
			
		}
		
	}
	
	protected void initNodeCoordinate(VisualizeGraph vg)
	{
		
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			int pos = ids[0];
			int labelId = ids[1];
			
			double y = -1;
			if(ids[4]==NODE_TYPES.entLEAF.ordinal()){
				y = 0;
			}else if (ids[4]==NODE_TYPES.tagLEAF.ordinal()){
				y = 0;
			}else if (ids[4]==NODE_TYPES.entNODE.ordinal()){
				y = labelId + 10;
			}else if (ids[4]==NODE_TYPES.tagNODE.ordinal()){
				y = labelId - 10;
			}else if (ids[4]==NODE_TYPES.ROOT.ordinal()){
				y=0;
			}
			
			double x = pos*1.5;
			
			
			
			//System.err.println(Arrays.toString(ids)+" coordinate:"+x+","+y);
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);

		}
	}

}
