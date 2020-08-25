/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jfree.chart;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.TickType;
import org.jfree.chart.axis.ValueTick;
import org.jfree.data.Range;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 *
 * @author Peter
 */
@SuppressWarnings("serial")
public class TriangleRightAxis extends TriangleAxis {

    public TriangleRightAxis(String label){
        super(label);
    }

    public TriangleRightAxis(){
        super();
    }
    protected AxisState drawLabel(String label, Graphics2D g2,
            Rectangle2D plotArea, Rectangle2D dataArea, RectangleEdge edge,
            AxisState state) {

        // it is unlikely that 'state' will be null, but check anyway...
        if (state == null) {
            throw new IllegalArgumentException("Null 'state' argument.");
        }

        if ((label == null) || (label.equals(""))) {
            return state;
        }
        Font font = getLabelFont();
        RectangleInsets insets = getLabelInsets();
        g2.setFont(font);
        g2.setPaint(getLabelPaint());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D labelBounds = TextUtilities.getTextBounds(label, g2, fm);
        double labely = dataArea.getMaxY() - dataArea.getWidth()/4.0*Math.tan(Math.PI/3);
        double offsetX = Math.abs(dataArea.getWidth()/4.0*Math.tan(Math.PI/3))/Math.tan(Math.PI/3);
        double labelAngle = Math.PI / 3.0;
        double sizeAngle = Math.PI / 2.0;
        AffineTransform t = AffineTransform.getRotateInstance(
                sizeAngle,
                labelBounds.getCenterX(), labelBounds.getCenterY());
        Shape rotatedLabelBounds = t.createTransformedShape(labelBounds);
        labelBounds = rotatedLabelBounds.getBounds2D();
        double labelx = state.getCursor()
                        + insets.getLeft() + labelBounds.getWidth() / 2.0;
        TextUtilities.drawRotatedString(label, g2, (float) labelx - (float)offsetX,
                (float) labely, TextAnchor.CENTER,
                labelAngle, TextAnchor.CENTER);
        state.cursorRight(insets.getLeft() + labelBounds.getWidth()
                + insets.getRight());
        return state;

    }

    protected float[] calculateAnchorPoint(ValueTick tick,
                                           double cursor,
                                           Rectangle2D dataArea,
                                           RectangleEdge edge) {

        RectangleInsets insets = getTickLabelInsets();
        float[] result = new float[2];
        result[1] = (float) valueToJava2D(tick.getValue(), dataArea, edge);
        double offsetY = dataArea.getMaxY()- result[1];
        double offsetX = offsetY / Math.tan(Math.PI / -3.0);
        result[0] = (float) (cursor + insets.getRight() + 2.0 + offsetX);
        return result;
    }

    public double valueToJava2D(double value, Rectangle2D area,
                                RectangleEdge edge) {

        Range range = getRange();
        double axisMin = range.getLowerBound();
        double axisMax = range.getUpperBound();

        double min = 0.0;
        double max = 0.0;
        min = area.getMaxY();
        double width = area.getWidth();
        max = min +  Math.tan(Math.PI / - 3.0) * width / 2.0;
        if (isInverted()) {
            return max
                   - ((value - axisMin) / (axisMax - axisMin)) * (max - min);
        }
        else {
            return min
                   + ((value - axisMin) / (axisMax - axisMin)) * (max - min);
        }

    }
    protected void drawAxisLine(Graphics2D g2, double cursor,
            Rectangle2D dataArea, RectangleEdge edge) {

        Line2D axisLine = null;
        double width = dataArea.getWidth();
        double height = dataArea.getHeight();
        double startX = cursor;
        double startY = dataArea.getY() + height;
        double endX = cursor -  width / 2.0;
        double sign = Math.signum(Math.tan(Math.PI / -3.0));
            //System.out.println("Signum " + sign);
        double endY = startY - Math.tan(Math.PI / -3.0) * width / 2.0 * sign ;
        axisLine = new Line2D.Double(startX, startY, endX, endY);
        g2.setPaint(getAxisLinePaint());
        g2.setStroke(getAxisLineStroke());
        g2.draw(axisLine);
    }

    @SuppressWarnings("rawtypes")
	protected AxisState drawTickMarksAndLabels(Graphics2D g2,
            double cursor, Rectangle2D plotArea, Rectangle2D dataArea,
            RectangleEdge edge) {

        AxisState state = new AxisState(cursor);

        if (isAxisLineVisible()) {
            drawAxisLine(g2, cursor, dataArea, edge);
        }

        List ticks = refreshTicks(g2, state, dataArea, edge);
        state.setTicks(ticks);
        g2.setFont(getTickLabelFont());
        Iterator iterator = ticks.iterator();
        while (iterator.hasNext()) {
            ValueTick tick = (ValueTick) iterator.next();
            if (isTickLabelsVisible()) {
                g2.setPaint(getTickLabelPaint());
                float[] anchorPoint = calculateAnchorPoint(tick, cursor,
                        dataArea, edge);
                //TextUtilities.drawRotatedString(tick.getText(), g2,
                //        anchorPoint[0], anchorPoint[1], tick.getTextAnchor(),
                //        tick.getAngle(), tick.getRotationAnchor());
                FontMetrics fm = g2.getFontMetrics(getTickLabelFont());
//                for (int i = 0; i < tick.getTextAsLines().length; i++) {
                int i = 0;
                    float a0 = (float) (anchorPoint[0] + i
                        * Math.sin(tick.getAngle())
                        * fm.getHeight());
                    float a1 = (float) (anchorPoint[1] + i
                        * Math.cos(tick.getAngle())
                        * fm.getHeight());

                    TextUtilities.drawRotatedString(tick.getText(),
                        g2, a0,
                        a1, tick.getTextAnchor(),
                        tick.getAngle(), tick.getRotationAnchor());
//                }
            }

            if ((isTickMarksVisible() && tick.getTickType().equals(
                    TickType.MAJOR)) || (isMinorTickMarksVisible()
                    && tick.getTickType().equals(TickType.MINOR))) {

                double ol = (tick.getTickType().equals(TickType.MINOR)) ?
                    getMinorTickMarkOutsideLength() : getTickMarkOutsideLength();

                double il = (tick.getTickType().equals(TickType.MINOR)) ?
                    getMinorTickMarkInsideLength() : getTickMarkInsideLength();

                float xx = (float) valueToJava2D(tick.getValue(), dataArea,
                        edge);
                Line2D mark = null;
                g2.setStroke(getTickMarkStroke());
                g2.setPaint(getTickMarkPaint());
                double offsetX = 0.0;
                double offsetY = dataArea.getMaxY()- xx;
                offsetX = offsetY / Math.tan(Math.PI / -3.0);
                mark = new Line2D.Double(cursor + offsetX + ol, xx, cursor + offsetX - il, xx);
                g2.draw(mark);
            }
        }

        // need to work out the space used by the tick labels...
        // so we can update the cursor...
        double used = 0.0;
        if (isTickLabelsVisible()) {
            if (edge == RectangleEdge.LEFT) {
                used += findMaximumTickLabelWidth(ticks, g2, plotArea,
                        isVerticalTickLabels());
                state.cursorLeft(used);
            }
            else if (edge == RectangleEdge.RIGHT) {
                used = findMaximumTickLabelWidth(ticks, g2, plotArea,
                        isVerticalTickLabels());
                state.cursorRight(used);
            }
            else if (edge == RectangleEdge.TOP) {
                used = findMaximumTickLabelHeight(ticks, g2, plotArea,
                        isVerticalTickLabels());
                state.cursorUp(used);
            }
            else if (edge == RectangleEdge.BOTTOM) {
                used = findMaximumTickLabelHeight(ticks, g2, plotArea,
                        isVerticalTickLabels());
                state.cursorDown(used);
            }
        }

        return state;
    }

}
