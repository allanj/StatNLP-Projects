/*
 * MyMouseMenus.java
 *
 * Created on March 21, 2007, 3:34 PM; Updated May 29, 2007
 *
 * Copyright March 21, 2007 Grotto Networking
 *
 */

package com.statnlp.ui.visualize.plugin;

import edu.uci.ics.jung.visualization.VisualizationViewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.statnlp.ui.visualize.EdgeMenuListener;
import com.statnlp.ui.visualize.EdgePropertyDialog;
import com.statnlp.ui.visualize.MenuPointListener;
import com.statnlp.ui.visualize.VertexMenuListener;
import com.statnlp.ui.visualize.VertexPropertyDialog;
import com.statnlp.ui.visualize.type.VLink;
import com.statnlp.ui.visualize.type.VNode;
import com.statnlp.ui.visualize.type.VisualizeGraph;

/**
 * A collection of classes used to assemble popup mouse menus for the custom
 * edges and vertices developed in this example.
 * 
 * @author Dr. Greg M. Bernstein
 */
public class MyMouseMenus {

	public static class EdgeMenu extends JPopupMenu {
		// private JFrame frame;
		public EdgeMenu(final JFrame frame) {
			super("Edge Menu");
			// this.frame = frame;
			this.addSeparator();
			this.add(new EdgePropItem(frame));
			this.add(new EdgeDelete());
		}

	}

	public static class EdgePropItem extends JMenuItem implements
			EdgeMenuListener<VLink>,
			MenuPointListener {
		VLink link;
		VisualizationViewer visComp;
		Point2D point;
		VisualizeGraph evg;

		public void setEdgeAndView(VLink link,
				VisualizationViewer visComp, VisualizeGraph evg) {
			this.link = link;
			this.visComp = visComp;
			this.evg = evg;
		}

		public void setPoint(Point2D point) {
			this.point = point;
		}

		public EdgePropItem(final JFrame frame) {
			super("Edit Edge Properties...");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					EdgePropertyDialog dialog = new EdgePropertyDialog(frame,
							link);
					dialog.setLocation((int) point.getX() + frame.getX(),
							(int) point.getY() + frame.getY());
					dialog.setVisible(true);
				}

			});
		}

	}
	
	public static class EdgeDelete extends JMenuItem implements EdgeMenuListener<VLink> {
		VLink link;
		VisualizationViewer vv;
		VisualizeGraph evg;
		
		public EdgeDelete() {
			super("Delete Edge");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					evg.removeEdge(link);
					vv.repaint();
					System.out.println("Delete the edge: " + link);
				}

			});
		}
		
		public void setEdgeAndView(VLink link, VisualizationViewer visComp, VisualizeGraph evg) {
			this.link = link;
			this.vv = visComp;
			this.evg = evg;
	    }
	}
	    
	  
	    

	public static class VertexMenu extends JPopupMenu {
		public VertexMenu(final JFrame frame) {
			super("Vertex Menu");
			this.addSeparator();
			this.add(new VertexPropItem(frame));
			this.add(new VertexDelete());

		}
	}

	public static class VertexPropItem extends JMenuItem implements VertexMenuListener<VNode>, MenuPointListener {
		VNode node;
		VisualizationViewer visComp;
		Point2D point;
		VisualizeGraph evg;

		public void setPoint(Point2D point) {
			this.point = point;
		}

		public VertexPropItem(final JFrame frame) {
			super("Edit Node Properties...");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					VertexPropertyDialog dialog = new VertexPropertyDialog(frame, node);
					dialog.setLocation((int) point.getX() + frame.getX(),
							(int) point.getY() + frame.getY());
					dialog.setVisible(true);
				}

			});
		}
		

		@Override
		public void setVertexAndView(VNode node, VisualizationViewer visComp, VisualizeGraph evg) {
			// TODO Auto-generated method stub
			this.node = node;
			this.visComp = visComp;
			this.evg = evg;
		}

	}
	
	public static class VertexDelete extends JMenuItem implements VertexMenuListener<VNode> {
		VNode node;
		VisualizationViewer vv;
		VisualizeGraph evg;
		
		public VertexDelete() {
			super("Delete the Vertex");
			this.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					evg.removeNode(node);
					vv.repaint();
					System.out.println("Delete the vertex : " + node);
				}

			});
		}
		

		@Override
		public void setVertexAndView(VNode node, VisualizationViewer visComp, VisualizeGraph evg) {
			// TODO Auto-generated method stub
			this.node = node;
			this.vv = visComp;
			this.evg = evg;
			
		}

	
		
	}

}
