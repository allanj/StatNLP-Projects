package com.statnlp.ui.visualize.plugin;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.picking.PickedState;

import com.statnlp.ui.MVCModel;
import com.statnlp.ui.MVCViewer;
import com.statnlp.ui.visualize.EdgePropertyDialog;
import com.statnlp.ui.visualize.ExpVisualizationEngine;
import com.statnlp.ui.visualize.VertexPropertyDialog;
import com.statnlp.ui.visualize.type.VLink;
import com.statnlp.ui.visualize.type.VNode;


public class MyPickingGraphMousePlugin<V, E> extends PickingGraphMousePlugin<V, E>{
	
	ExpVisualizationEngine eve = null;
	
	JFrame frame;
	
	public MyPickingGraphMousePlugin(ExpVisualizationEngine eve)
	{
		super();
		setExpVisualizationEngine(eve);
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		super.mouseReleased(e);
		eve.markPickedNodes();
		//System.out.println("Marked Picked Nodes");
	}
	
	public void setExpVisualizationEngine(ExpVisualizationEngine eve)
	{
		this.eve = eve;
	}
	
	public void setFrame(JFrame frame)
	{
		this.frame = frame;
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		  if(e.getClickCount()==2){

			  final VisualizationViewer<VNode,VLink> vv =
		                (VisualizationViewer<VNode,VLink>)e.getSource();
		        Point2D p = e.getPoint();
		        
		        GraphElementAccessor<VNode,VLink> pickSupport = vv.getPickSupport();
		        if(pickSupport != null) {
		            VNode v = pickSupport.getVertex(vv.getGraphLayout(), p.getX(), p.getY());
		            if(v != null) {
		                // System.out.println("Vertex " + v + " was right clicked");
		                //updateVertexMenu(v, vv, p);
		                //vertexPopup.show(vv, e.getX(), e.getY());
		               
		                VertexPropertyDialog dialog = new VertexPropertyDialog(frame, v);
						dialog.setLocation((int) p.getX() + frame.getX(),
								(int) p.getY() + frame.getY());
						dialog.setVisible(true);
						
		            } else {
		                VLink edge = pickSupport.getEdge(vv.getGraphLayout(), p.getX(), p.getY());
		            
		                if(edge != null) {
		                    // System.out.println("Edge " + edge + " was right clicked");
		                    //updateEdgeMenu(edge, vv, p);
		                    //edgePopup.show(vv, e.getX(), e.getY());
		                    EdgePropertyDialog dialog = new EdgePropertyDialog(frame,
									edge);
							dialog.setLocation((int) p.getX() + frame.getX(),
									(int) p.getY() + frame.getY());
							dialog.setVisible(true);
		                  
		                }
		            }
		        }
	      }
	}
	
	
	
	
}
