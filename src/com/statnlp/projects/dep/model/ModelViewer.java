package com.statnlp.projects.dep.model;

import java.awt.Color;
import java.awt.geom.Point2D;

import com.statnlp.ui.visualize.VisualizationViewerEngine;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

public class ModelViewer extends VisualizationViewerEngine {


	
	private String[] types;
	
	public ModelViewer(int TypeLength) {
		super(TypeLength);
	}
	
	public ModelViewer(int TypeLength, String[] types) {
		super(TypeLength);
		this.types = types;
	}

	protected void initTypeColorMapping()
	{	
		colorMap[0] = Color.WHITE;  //general, complete
		colorMap[1] = Color.GREEN;  //general incomplete
		colorMap[2] = Color.BLUE;  //labeled, complete
		colorMap[3] = Color.YELLOW; //labeled, incomplete
		
	}
	
	
	@Override
	protected String label_mapping(int[] ids) {
		int rightIndex = ids[0];
		int leftIndex = ids[0] - ids[1];
		
		String completeness = ids[2]==1? "com":"incom";
		String dir = ids[3]==1? "RD": "LD";
		String type = null;
		
		if(ids[4]==2*types.length)
			type = "pae:null";
		else if(ids[4]>types.length-1){
			type = "pae:"+types[ids[4]-types.length];
		}else{
			type = types[ids[4]];
		}
		String label = leftIndex+","+rightIndex+","+completeness+","+dir+","+type;
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
			
			String completeness = ids[2]==1? "com":"incom";
			//String dir = ids[3]==1? "RD": "LD";
			String type = null;
			
			if(ids[4]==2*types.length)
				type = "pae:null";
			else if(ids[4]>types.length-1){
				type = "pae:"+types[ids[4]-types.length];
			}else{
				type = types[ids[4]];
			}
			
			
			int color_index = -1;
			if(type.startsWith("pae") && completeness.equals("com")){
				color_index = 0;
			}else if(type.startsWith("pae") && completeness.equals("incom")){
				color_index = 1;
			}else if(!type.startsWith("pae") && completeness.equals("com")){
				color_index = 2;
			}else if(!type.startsWith("pae") && completeness.equals("incom")){
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
			int rightIndex = ids[0];
			int leftIndex = ids[0] - ids[1];
			
			String completeness = ids[2]==1? "com":"incom";
			String dir = ids[3]==1? "RD": "LD";
			String type = null;
			
			if(ids[4]==2*types.length)
				type = "pae:null";
			else if(ids[4]>types.length-1){
				type = "pae:"+types[ids[4]-types.length];
			}else{
				type = types[ids[4]];
			}
			
			double x = (leftIndex+rightIndex)/2.0 * 1000;
			double y = -(rightIndex-leftIndex)*1000;
			
			if(leftIndex==rightIndex){
				if(dir.equals("RD"))
					x+=200;
				else x-=200;
			}
			if(leftIndex!=rightIndex ){
				if(dir.equals("RD"))
					x+=10;
				else x-=10;
			}
			
			if(type.startsWith("pae")) y-=300;
			
			if(completeness.equals("incom")) y+=500;
			if(type.equals("ONE") || type.equals("pae:ONE")){
				y-=50;
			}else if(type.equals("OE")|| type.equals("pae:OE")){
				y-=100;
			}else if(type.equals("person")|| type.equals("pae:person")){
				y-=150;
			}else if(type.equals("gpe")|| type.equals("pae:gpe")){
				y-=200;
			}else if(type.equals("organization")|| type.equals("pae:organization")){
				y-=250;
			}
			
			//System.err.println(Arrays.toString(ids)+" coordinate:"+x+","+y);
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);

		}
	}

}
