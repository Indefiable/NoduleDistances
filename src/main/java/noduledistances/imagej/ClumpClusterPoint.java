package noduledistances.imagej;



import java.awt.geom.Point2D;

import org.apache.commons.math3.ml.clustering.Clusterable;


/**
 * An implementation of Apache Common's Clusterable interface to use 
 * Apache's clustering methods in RoiOverlay.breakupClump()
 * 
 * 
 * @author Brandin Farris
 *
 */
public class ClumpClusterPoint implements Clusterable {
	Point2D.Double p;
	double x;
	double y;
	
	public ClumpClusterPoint(Point2D.Double p) {
		this.p = p;
		this.x = p.x;
		this.y = p.y;
	}
	
	public ClumpClusterPoint(double x, double y) {
		this.p = new Point2D.Double(x,y);
		this.x = p.x;
		this.y = p.y;
	}

	@Override
	public double[] getPoint() {
		return new double[] {p.x,p.y};
	}

}
