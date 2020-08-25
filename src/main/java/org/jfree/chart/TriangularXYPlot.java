package org.jfree.chart;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.TriangularXYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

@SuppressWarnings({"serial", "rawtypes", "deprecation"})
public class TriangularXYPlot extends XYPlot {

    private Rectangle2D dataArea;
	private XYItemRenderer r;
	private static boolean DEBUG = true;

	public static void setDebug(boolean debug){
		DEBUG = debug;
	}
	public TriangularXYPlot() {
    	this("x", "y", "z");
    }
    
    public TriangularXYPlot(String xlabel, String ylabel, String zlabel) {
        super();
//        this.datasets = new ObjectList();
//        this.renderers = new ObjectList();

        TriangleBottomAxis bottom = new TriangleBottomAxis(xlabel);
        TriangleLeftAxis left = new TriangleLeftAxis(ylabel);
        TriangleRightAxis right = new TriangleRightAxis(zlabel);
        
        bottom.setTickUnit(new NumberTickUnit(0.1));
        left.setTickUnit(new NumberTickUnit(0.1));
        right.setTickUnit(new NumberTickUnit(0.1));
        
        r =  new TriangularXYLineAndShapeRenderer();
        setRenderer(r);
        setDomainAxis(bottom);
        setRangeAxis(left);
        setRangeAxis(1, right);
    }
    
    public void setDomainAxis(ValueAxis axis) {
        if (axis instanceof TriangleBottomAxis) {
            axis.setInverted(false);
            super.setDomainAxis(axis);
        }
    }

    public void setDomainAxis(int index, ValueAxis axis) {
        if (axis instanceof TriangleBottomAxis) {
            axis.setInverted(false);
            super.setDomainAxis(0, axis);
        }
    }

    public void setRangeAxis(ValueAxis axis) {
        if (axis instanceof TriangleLeftAxis) {
        	axis.setInverted(true);
            super.setRangeAxis(axis);
        }
    }

    public void setRangeAxis(int index, ValueAxis axis) {
        if (axis instanceof TriangleLeftAxis && index == 0) {
        	axis.setInverted(true);
            super.setRangeAxis(index, axis);
        } else if (axis instanceof TriangleRightAxis && index > 0) {
            axis.setInverted(false);
            super.setRangeAxis(1, axis);
        }
    }

    /**
     * @param x the java 2D x coordinate (divided by the panel getXScale())
     * @param y the java 2D x coordinate (divided by the panel getYScale())
     * @return the value in the data coordinate space
     */
    public Point2D java2DToValue(double x, double z){
		double xvalue = 1 - (x-dataArea.getMinX())/dataArea.getWidth();
		xvalue = Math.min(0.99, Math.max(0.01, xvalue));
		
		double zvalue = 1 - (z-dataArea.getMinY())/dataArea.getHeight();
		zvalue = Math.min(0.99, Math.max(0.01, zvalue));
		
		xvalue -= zvalue/2;
		double yvalue = 1-xvalue-zvalue;
		
		return new Point2D.Double(yvalue, xvalue);
    }

	public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        // if the plot area is too small, just return...
		if (DEBUG){
			System.out.println("In TriangularXYPlot.draw");
		}
        boolean b1 = (area.getWidth() <= MINIMUM_WIDTH_TO_DRAW);
        boolean b2 = (area.getHeight() <= MINIMUM_HEIGHT_TO_DRAW);
        if (b1 || b2) {
            return;
        }

        // record the plot area...
        if (info != null) {
            info.setPlotArea(area);
        }

        // adjust the drawing area for the plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);
        AxisSpace space = calculateAxisSpace(g2, area);
        dataArea = space.shrink(area, null);
        getAxisOffset().trim(dataArea);
        double requiredHeight = dataArea.getWidth() / 2.0 * Math.tan(Math.PI / 3.0);
        if(requiredHeight > dataArea.getHeight()){
            double newWidth = dataArea.getHeight() / Math.tan(Math.PI / 3.0) * 2.0;
            double widthDiff = dataArea.getWidth() - newWidth;
            double newHeight = newWidth / 2.0 * Math.tan(Math.PI / 3.0);
            double heightDiff = dataArea.getHeight() - newHeight;
            double startX = dataArea.getX() + widthDiff / 2.0;
            double startY = dataArea.getY() + heightDiff / 2.0;
    		if (DEBUG){
                System.out.println("Required height " + requiredHeight);
                System.out.println("Available height " + dataArea.getHeight());
                System.out.println("New height " + newHeight);
                System.out.println("New width " + newWidth);
                System.out.println("Available width " + dataArea.getWidth());
    		}
            dataArea = new Rectangle2D.Double(startX, startY, newWidth, newHeight);
        }
        createAndAddEntity((Rectangle2D) dataArea.clone(), info, null, null);
        if (info != null) {
            info.setDataArea(dataArea);
        }

        // draw the plot background and axes...
        drawBackground(g2, dataArea);
        Map axisStateMap = drawAxes(g2, area, dataArea, info);

        PlotOrientation orient = getOrientation();

        // the anchor point is typically the point where the mouse last
        // clicked - the crosshairs will be driven off this point...
        if (anchor != null && !dataArea.contains(anchor)) {
            anchor = null;
        }
        CrosshairState crosshairState = new CrosshairState();
        crosshairState.setCrosshairDistance(Double.POSITIVE_INFINITY);
        crosshairState.setAnchor(anchor);

        crosshairState.setAnchorX(Double.NaN);
        crosshairState.setAnchorY(Double.NaN);
        if (anchor != null) {
            ValueAxis domainAxis = getDomainAxis();
            if (domainAxis != null) {
                double x;
                if (orient == PlotOrientation.VERTICAL) {
                    x = domainAxis.java2DToValue(anchor.getX(), dataArea,
                            getDomainAxisEdge());
                } else {
                    x = domainAxis.java2DToValue(anchor.getY(), dataArea,
                            getDomainAxisEdge());
                }
                crosshairState.setAnchorX(x);
            }
            ValueAxis rangeAxis = getRangeAxis();
            if (rangeAxis != null) {
                double y;
                if (orient == PlotOrientation.VERTICAL) {
                    y = rangeAxis.java2DToValue(anchor.getY(), dataArea,
                            getRangeAxisEdge());
                } else {
                    y = rangeAxis.java2DToValue(anchor.getX(), dataArea,
                            getRangeAxisEdge());
                }
                crosshairState.setAnchorY(y);
            }
        }
        crosshairState.setCrosshairX(getDomainCrosshairValue());
        crosshairState.setCrosshairY(getRangeCrosshairValue());
        Shape originalClip = g2.getClip();
        Composite originalComposite = g2.getComposite();

        g2.clip(dataArea);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        AxisState domainAxisState = (AxisState) axisStateMap.get(
                getDomainAxis());
        if (domainAxisState == null) {
            if (parentState != null) {
                domainAxisState = (AxisState) parentState.getSharedAxisStates().get(getDomainAxis());
            }
        }

        AxisState leftRangeAxisState = (AxisState) axisStateMap.get(getRangeAxis());
        if (leftRangeAxisState == null) {
            if (parentState != null) {
                leftRangeAxisState = (AxisState) parentState.getSharedAxisStates().get(getRangeAxis());
            }
        }
        AxisState rightRangeAxisState = (AxisState) axisStateMap.get(getRangeAxis(1));
        if (rightRangeAxisState == null) {
            if (parentState != null) {
                rightRangeAxisState = (AxisState) parentState.getSharedAxisStates().get(getRangeAxis(1));
            }
        }
        if (domainAxisState != null) {
            drawDomainTickBands(g2, dataArea, domainAxisState.getTicks());
        }
        if (leftRangeAxisState != null) {
            drawRangeTickBands(g2, dataArea, leftRangeAxisState.getTicks());
        }
        if (domainAxisState != null) {
            drawDomainGridlines(g2, dataArea, domainAxisState.getTicks());
            drawZeroDomainBaseline(g2, dataArea);
        }
        if (leftRangeAxisState != null) {
            drawLeftRangeGridlines(g2, dataArea, leftRangeAxisState.getTicks());
            drawZeroRangeBaseline(g2, dataArea);
        }
        if (rightRangeAxisState != null) {
            drawRightRangeGridlines(g2, dataArea, rightRangeAxisState.getTicks());
        }

        // draw the markers that are associated with a specific renderer...
        for (int i = 0; i < getRendererCount(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }
        for (int i = 0; i < getRendererCount(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }

        // now draw annotations and render data items...
        boolean foundData = false;
        DatasetRenderingOrder order = getDatasetRenderingOrder();
        if (order == DatasetRenderingOrder.FORWARD) {

            // draw background annotations
            for (int i = 0; i < getRendererCount(); i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            // render data items...
            for (int i = 0; i < getDatasetCount(); i++) {
            	if (DEBUG){
            		System.out.println("Rendering dataset FORWARD" + i);
            	}
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = 0; i < getRendererCount(); i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        } else if (order == DatasetRenderingOrder.REVERSE) {

            // draw background annotations
            for (int i = getRendererCount() - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (i >= getDatasetCount()) { // we need the dataset to make
                    continue;                 // a link to the axes
                }
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            for (int i = getDatasetCount() - 1; i >= 0; i--) {
            	if (DEBUG){
            		System.out.println("Rendering dataset REVERSE" + i);
            	}
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = getRendererCount() - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (i >= getDatasetCount()) { // we need the dataset to make
                    continue;                 // a link to the axes
                }
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        }

        // draw domain crosshair if required...
        int xAxisIndex = crosshairState.getDomainAxisIndex();
        ValueAxis xAxis = getDomainAxis(xAxisIndex);
        RectangleEdge xAxisEdge = getDomainAxisEdge(xAxisIndex);
        if (!isDomainCrosshairLockedOnData() && anchor != null) {
            double xx;
            if (orient == PlotOrientation.VERTICAL) {
                xx = xAxis.java2DToValue(anchor.getX(), dataArea, xAxisEdge);
            } else {
                xx = xAxis.java2DToValue(anchor.getY(), dataArea, xAxisEdge);
            }
            crosshairState.setCrosshairX(xx);
        }
        setDomainCrosshairValue(crosshairState.getCrosshairX(), false);
        if (isDomainCrosshairVisible()) {
            double x = getDomainCrosshairValue();
            Paint paint = getDomainCrosshairPaint();
            Stroke stroke = getDomainCrosshairStroke();
            drawDomainCrosshair(g2, dataArea, orient, x, xAxis, stroke, paint);
        }

        // draw range crosshair if required...
        //System.out.println("[XYPlot.draw] domain crosshair value " + getDomainCrosshairValue());
        //System.out.println("[XYPlot.draw] range crosshair value " + getRangeCrosshairValue());
        //System.out.println("[XYPlot.draw] anchor " + anchor);
        int yAxisIndex = crosshairState.getRangeAxisIndex();
        ValueAxis yAxis = getRangeAxis(yAxisIndex);
        RectangleEdge yAxisEdge = getRangeAxisEdge(yAxisIndex);
        if (!isRangeCrosshairLockedOnData() && anchor != null) {
            double yy;
            if (orient == PlotOrientation.VERTICAL) {
                yy = yAxis.java2DToValue(anchor.getY(), dataArea, yAxisEdge);
            } else {
                yy = yAxis.java2DToValue(anchor.getX(), dataArea, yAxisEdge);
            }
            crosshairState.setCrosshairY(yy);
        }
        setRangeCrosshairValue(crosshairState.getCrosshairY(), false);
        if (isRangeCrosshairVisible()) {
            double y = getRangeCrosshairValue();
            Paint paint = getRangeCrosshairPaint();
            Stroke stroke = getRangeCrosshairStroke();
            drawRangeCrosshair(g2, dataArea, orient, y, yAxis, stroke, paint);
        }

        if (!foundData) {
            drawNoDataMessage(g2, dataArea);
        }

        for (int i = 0; i < getRendererCount(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }
        for (int i = 0; i < getRendererCount(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }

        drawAnnotations(g2, dataArea, info);
        g2.setClip(originalClip);
        g2.setComposite(originalComposite);

//        drawOutline(g2, dataArea);

    }

    public void drawBackground(Graphics2D g2, Rectangle2D area) {
        Path2D.Double gp = new Path2D.Double();
        gp.moveTo(area.getX(), area.getMaxY());
        gp.lineTo(area.getMaxX(), area.getMaxY());
        gp.lineTo(area.getX() + area.getWidth() / 2.0, area.getMaxY() - Math.tan(0.333 * Math.PI) * area.getWidth() / 2.0);
        gp.lineTo(area.getX(), area.getMaxY());
        gp.closePath();
        g2.setPaint(getBackgroundPaint());
        g2.fill(gp);
    }

    protected void drawDomainGridlines(Graphics2D g2, Rectangle2D dataArea, List ticks) {
        // draw the domain grid lines, if any...
        if (isDomainGridlinesVisible() || isDomainMinorGridlinesVisible()) {
			Stroke gridStroke = null;
            Paint gridPaint = null;
            Iterator iterator = ticks.iterator();
            boolean paintLine = false;
            while (iterator.hasNext()) {
                paintLine = false;
                ValueTick tick = (ValueTick) iterator.next();
                if ((tick.getTickType() == TickType.MINOR) && isDomainMinorGridlinesVisible()) {
                    gridStroke = getDomainMinorGridlineStroke();
                    gridPaint = getDomainMinorGridlinePaint();
                    paintLine = false;
                } else if ((tick.getTickType() == TickType.MAJOR) && isDomainGridlinesVisible()) {
                    gridStroke = getDomainGridlineStroke();
                    gridPaint = getDomainGridlinePaint();
                    paintLine = true;
                }
                if (paintLine) {
                	g2.setStroke(gridStroke);
                	g2.setPaint(gridPaint);
                	
                    double startX = getDomainAxis().valueToJava2D(tick.getValue(), dataArea, RectangleEdge.BOTTOM);
                    double lenX = startX - dataArea.getMinX();
                    //double endX =  dataArea.getWidth() / 2.0 + lenX / 2.0 + dataArea.getMinX();
                    double endX = dataArea.getMinX() + lenX / 2.0;
                    double startY = dataArea.getMaxY();
                    //double endY = startY - Math.tan(Math.PI/180*60) * (dataArea.getMaxX() - (dataArea.getWidth() / 2.0 + lenX / 2.0 + dataArea.getMinX()));
                    double endY = startY - Math.tan(Math.PI / 180 * 60) * lenX / 2.0;
                    if(tick.getValue()!=1 && tick.getValue()!=0){
                    	g2.draw(new Line2D.Double(startX, startY, endX, endY));
                    
                    	// draw the orientation reading ticks
                    	ValueAxis axis = getRangeAxis();
                    	g2.setPaint(axis.getTickMarkPaint());
                    	g2.setStroke(axis.getTickMarkStroke());
                    	double l = axis.getTickMarkOutsideLength()*2;
                    	g2.draw(new Line2D.Double(endX, endY, endX + l, endY+Math.sqrt(4*l*l-l*l)));
                    }
                }
            }
        }
    }

    protected void drawLeftRangeGridlines(Graphics2D g2, Rectangle2D dataArea, List ticks) {
        // draw the domain grid lines, if any...
        if (isRangeGridlinesVisible() || isRangeMinorGridlinesVisible()) {
            Stroke gridStroke = null;
            Paint gridPaint = null;
            Iterator iterator = ticks.iterator();
            boolean paintLine = false;
            while (iterator.hasNext()) {
                paintLine = false;
                ValueTick tick = (ValueTick) iterator.next();
                if ((tick.getTickType() == TickType.MINOR) && isDomainMinorGridlinesVisible()) {
                    gridStroke = getRangeMinorGridlineStroke();
                    gridPaint = getRangeMinorGridlinePaint();
                    paintLine = false;
                } else if ((tick.getTickType() == TickType.MAJOR) && isDomainGridlinesVisible()) {
                    gridStroke = getRangeGridlineStroke();
                    gridPaint = getRangeGridlinePaint();
                    paintLine = true;
                }
                if (paintLine) {
                	g2.setStroke(gridStroke);
                	g2.setPaint(gridPaint);
                	
                    double startY = getRangeAxis(1).valueToJava2D(tick.getValue(), dataArea, RectangleEdge.RIGHT);
                    double topY = dataArea.getMaxY() - dataArea.getWidth() / 2.0 * Math.tan(Math.PI / 180.0 * 60.0);
                    double endY = dataArea.getMaxY();
                    double yLen = startY - topY;
                    double offsetX = yLen / Math.tan(Math.PI / 180.0 * 60.0);
                    double startX = dataArea.getMinX() + dataArea.getWidth() / 2.0 + offsetX;
                    double endX = dataArea.getMinX() + offsetX * 2.0;
                    if(tick.getValue()!=1 && tick.getValue()!=0){
                    	g2.draw(new Line2D.Double(startX, startY, endX, endY));
                    
                    	// draw the orientation reading ticks
                    	ValueAxis axis = getRangeAxis();
                    	g2.setPaint(axis.getTickMarkPaint());
                    	g2.setStroke(axis.getTickMarkStroke());
                    	double l = axis.getTickMarkOutsideLength()*2;
                    	g2.draw(new Line2D.Double(endX, endY, endX + l,endY-Math.sqrt(4*l*l-l*l)));
                    }
                }
            }
        }
    }

    protected void drawRightRangeGridlines(Graphics2D g2, Rectangle2D dataArea, List ticks) {
        // draw the domain grid lines, if any...
        if (isRangeGridlinesVisible() || isRangeMinorGridlinesVisible()) {
            Stroke gridStroke = null;
            Paint gridPaint = null;
            Iterator iterator = ticks.iterator();
            boolean paintLine = false;
            while (iterator.hasNext()) {
                paintLine = false;
                ValueTick tick = (ValueTick) iterator.next();
                if ((tick.getTickType() == TickType.MINOR) && isDomainMinorGridlinesVisible()) {
                    gridStroke = getRangeMinorGridlineStroke();
                    gridPaint = getRangeMinorGridlinePaint();
                    paintLine = false;
                } else if ((tick.getTickType() == TickType.MAJOR) && isDomainGridlinesVisible()) {
                    gridStroke = getRangeGridlineStroke();
                    gridPaint = getRangeGridlinePaint();
                    paintLine = true;
                }
                if (paintLine) {
                	g2.setStroke(gridStroke);
                	g2.setPaint(gridPaint);
                	
                    double commonY = getRangeAxis().valueToJava2D(tick.getValue(), dataArea, RectangleEdge.LEFT);
                    double lenY = commonY - dataArea.getMaxY();
                    double offsetX = lenY / Math.tan(Math.PI / 180 * 60);
                    //System.out.println("Offset X: "  + offsetX);
                    double startX = dataArea.getMinX() - offsetX;
                    double endX = dataArea.getMaxX() + offsetX;
                    if(tick.getValue()!=1 && tick.getValue()!=0){
                    	g2.draw(new Line2D.Double(startX, commonY, endX, commonY));
                    
                    	// draw the orientation reading ticks
                    	ValueAxis axis = getRangeAxis();
                    	g2.setPaint(axis.getTickMarkPaint());
                    	g2.setStroke(axis.getTickMarkStroke());
                    	double l = axis.getTickMarkOutsideLength()*4;
                    	g2.draw(new Line2D.Double(endX, commonY, endX - l, commonY));
                    }
                }
            }
        }
    }

    public void configureRangeAxes() {
        //System.out.println("Setting axes");
        if (getRangeAxis(0) != null) {
            getRangeAxis(0).configure();
        }
    }

    public void axisChanged(AxisChangeEvent ace) {
        if (ace.getAxis() != getRangeAxis(1)) {
            if (getRangeAxis() != null && getDomainAxis() != null && getRangeAxis(1) != null) {
                double upper = 100 - getDomainAxis().getLowerBound() - getRangeAxis().getLowerBound();
                double lower = 100 - getDomainAxis().getUpperBound() - getRangeAxis().getUpperBound();
                getRangeAxis(1).setLowerBound(Math.max(0.0, lower));
                getRangeAxis(1).setUpperBound(Math.min(100.0, upper));
            }
        }
        super.axisChanged(ace);
    }
    /**
     * Returns the renderer for the primary dataset.
     *
     * @return The item renderer (possibly <code>null</code>).
     *
     * @see #setRenderer(XYItemRenderer)
     */
//    public XYItemRenderer getRenderer() {
//        return getRenderer(0);
//    }

    /** Storage for the renderers. */
//    private ObjectList renderers;
    /** Storage for the datasets. */
//    private ObjectList datasets;

    /**
     * Returns the renderer for a dataset, or <code>null</code>.
     *
     * @param index  the renderer index.
     *
     * @return The renderer (possibly <code>null</code>).
     *
     * @see #setRenderer(int, XYItemRenderer)
     */
//    public XYItemRenderer getRenderer(int index) {
//        XYItemRenderer result = null;
//        if (this.renderers.size() > index) {
//            result = (XYItemRenderer) this.renderers.get(index);
//        }
//        return result;
//
//    }

}
