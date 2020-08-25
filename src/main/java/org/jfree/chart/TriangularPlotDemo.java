package org.jfree.chart;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;

public class TriangularPlotDemo {
	public static void main(String[] args) {
		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries("Blue circles",new double[][]{{0, 1, 0.5,0.1, 0.6, 0.2, 0}, {1, 0, 0.5, 0.2, 0.3, 0.6, 0}});		
        XYPlot plot = new TriangularXYPlot("ACETONE", "BENZENE", "TOLUENE");
		plot.setDataset(dataset);
		
		// adjust the renderer to draw circles
		XYItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-4, -4, 8, 8));
        
        JFreeChart chart = new JFreeChart("Triangular Demo", new Font("Tahoma", 2, 18), plot, true);
		JFrame frame = new JFrame("XY Plot Demo");
		ChartPanel cPanel = new ChartPanel(chart);
		frame.getContentPane().add(cPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}

