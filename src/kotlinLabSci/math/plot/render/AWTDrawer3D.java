package kotlinLabSci.math.plot.render;

import kotlinLabSci.math.plot.canvas.PlotCanvas;

public class AWTDrawer3D extends AWTDrawer {

	public AWTDrawer3D(PlotCanvas _canvas) {
		super(_canvas);
		projection = new Projection3D(this);
	}

	public void rotate(int[] t, int[] panelSize) {
		((Projection3D) projection).rotate(t, panelSize);
	}

}
