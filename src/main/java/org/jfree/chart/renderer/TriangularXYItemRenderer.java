/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jfree.chart.renderer;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

/*
 * @author Peter
 */

@SuppressWarnings("serial")
public class TriangularXYItemRenderer extends AbstractXYItemRenderer{

	boolean tieLine = true;
	
    public TriangularXYItemRenderer(){
        super();
    }
    
    public void setTieLine(boolean on){
    	this.tieLine = on;
    }
    
    public void drawItem(Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {

        Shape hotspot = null;
        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        double x = dataset.getXValue(series, item);
        double y = dataset.getYValue(series, item);
        if (Double.isNaN(x) || Double.isNaN(y)) {
            // can't draw anything
            return;
        }
        
//        TriangularXYPlot tplot = (TriangularXYPlot) plot;
        
        double topY = dataArea.getMaxY() - dataArea.getWidth() / 2.0 * Math.tan(Math.PI / 180.0 * 60.0);
        double endY = dataArea.getMaxY();
        
        double z = 1.0 - x - y;
        double offset = 0.5 * z * dataArea.getWidth();
        double transX =  dataArea.getX() + x*dataArea.getWidth()  + offset;
        double transY = topY + (1-z) * (endY-topY);

        PlotOrientation orientation = plot.getOrientation();
        Shape shape = getItemShape(series, item);
        if (orientation == PlotOrientation.HORIZONTAL) {
            shape = ShapeUtilities.createTranslatedShape(shape, transY,
                    transX);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            shape = ShapeUtilities.createTranslatedShape(shape, transX,
                    transY);
        }
        hotspot = shape;
        if (shape.intersects(dataArea)) {
                //if (getItemShapeFilled(series, item)) {
                    g2.setPaint(lookupSeriesPaint(series));
                    g2.fill(shape);
               //}
            g2.setPaint(getItemOutlinePaint(series, item));
            g2.setStroke(getItemOutlineStroke(series, item));
            g2.draw(shape);
        }
        
        if(tieLine && series%2==0 && dataset.getSeriesCount()>series+1){
            double x2 = dataset.getXValue(series+1, item);
            double y2 = dataset.getYValue(series+1, item);
            if (Double.isNaN(x2) || Double.isNaN(y2)) {
                // can't draw anything
                return;
            }
            
            double z2 = 1.0 - x2 - y2;
            double offset2 = 0.5 * z2*dataArea.getWidth();
            double transX2 =  dataArea.getX() + x2*dataArea.getWidth()  + offset2;
            double transY2 = topY + (1-z2) * (endY-topY);
            
            g2.drawLine((int)transX, (int)transY, (int)transX2, (int)transY2);
        }

        // add an entity for the item...
        if (entities != null) {
            addEntity(entities, hotspot, dataset, series, item, transX,
                    transY);
        }
    }
}
