
package org.jfree.chart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;

@SuppressWarnings("serial")
public class TriangleAxis extends NumberAxis {
    public TriangleAxis(String label){
        super(label);
    }

    public TriangleAxis(){
        super();
    }

    protected void autoAdjustRange(){
        setRange(new Range(0.0, 1.0), false, false);
    }
}
