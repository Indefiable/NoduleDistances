package noduledistances.imagej;

import java.awt.Color;
import java.awt.Font;


import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.process.ColorProcessor;


/**
 * This class is used to hold the image plus object that 
 * has the skeletonized root system on top of the image of the root.
 *  
 * @author Brandin Farris
 *
 */
public class GraphOverlay {
	
	
	private final int NODERADIUS=6;
	
	
	public ImagePlus overlayedGraph;
	
	
	
	public GraphOverlay() {
		
	}

	
	public void showGraph() {
		
		if(overlayedGraph != null) {
			overlayedGraph.show();
		}
		
	}
	
	
	/**
	 * overlays the graph and nodules onto the user-provided root system image. 
	 * Labels each nodule by the nodule naming system noduleNumber_noduleSubnumber
	 * 
	 * 
	 * @param graph : object that contains all information about the skeltonized root system.
	 * @param cp : ColorProcessor object of the root system image provided by the user.
	 */
	public void overlayGraph(RootGraph graph, ColorProcessor cp) {
		ImagePlus skellyMap = new ImagePlus("skeleton", cp);
		TextRoi.setFont("SansSerif",30 , Font.BOLD);
    	Font font = new Font("SansSerif",Font.BOLD,30);
    	  
		Overlay overlay = new Overlay();
		skellyMap.setOverlay(overlay);
		
		
		for(Node node : graph.nodes) {
			
			if(node.type < 1) {
			OvalRoi ball = new OvalRoi( node.x - NODERADIUS,  node.y - NODERADIUS, 2 * NODERADIUS, 2 * NODERADIUS);
			ball.setFillColor(Color.BLUE);
			skellyMap.getOverlay().add(ball);
			
		
			}
			
			else {
				
				String label = Double.toString(node.nodeNumber);

				String[] parts = label.split("\\.");
				String part2;
				
				if(parts.length > 1 && !parts[1].equals("0")) {
					parts[1] = Integer.toString(Integer.parseInt(parts[1]));
					part2 = "_" + parts[1];
				}
				else {
					part2 = "";
				}
				
				label = parts[0] + part2;
				
		    	TextRoi textLabel = new TextRoi(node.x,
			    			node.y, label,font);
		    	
		    	
		    	textLabel.setStrokeWidth(2); 
		    	
				int radius = NODERADIUS + 8;
				OvalRoi ball = new OvalRoi( node.x - radius,  node.y - radius, 2 * radius, 2 * radius);
				
				if(node.type ==1) {
					ball.setFillColor(Color.RED);
					textLabel.setStrokeColor(Color.RED); 
				}
				if(node.type == 2) {
					ball.setFillColor(Color.GREEN);
					textLabel.setStrokeColor(Color.GREEN); 
				}
				if(node.type == 3) {
					ball.setFillColor(Color.YELLOW);
					textLabel.setStrokeColor(Color.YELLOW); 
				}
				skellyMap.getOverlay().add(ball);
				skellyMap.getOverlay().add(textLabel);
		    	
			}
			
		}
		
		for (int[] edge : graph.fsRep) {	
			Node node1 = graph.nodes.get(edge[0]);
			Node node2 = graph.nodes.get(edge[1]);
			
		 	Line line = new Line(node1.x, node1.y, node2.x, node2.y);
		    line.setStrokeWidth(2);
		    line.setStrokeColor(Color.CYAN);
		    overlay.add(line);
		}
		
		
		skellyMap.setTitle("Graph");
		
		this.overlayedGraph = skellyMap;
		
	}
	
	
}
