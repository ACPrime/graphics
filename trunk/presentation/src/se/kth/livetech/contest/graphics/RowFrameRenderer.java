package se.kth.livetech.contest.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import se.kth.livetech.presentation.graphics.Renderable;

//TODO: make hashable
public class RowFrameRenderer implements Renderable{
	Color color1; 
	Color color2;
	public RowFrameRenderer(Color color1, Color color2){
		this.color1 = color1;
		this.color2 = color2;
	}
	public void render(Graphics2D g, Dimension d) {
		GradientPaint paint = new GradientPaint(0, 0, color1, d.width/2, 0, color2, true);
		Rectangle2D rect = new Rectangle2D.Float(0,0,d.width, d.height);
		g.setPaint(paint);
		g.fill(rect);
	}

}
