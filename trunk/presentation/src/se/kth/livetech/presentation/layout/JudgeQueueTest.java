package se.kth.livetech.presentation.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import se.kth.livetech.contest.graphics.ContentProvider;
import se.kth.livetech.contest.graphics.ICPCColors;
import se.kth.livetech.contest.graphics.ICPCImages;
import se.kth.livetech.contest.graphics.RowFrameRenderer;
import se.kth.livetech.contest.graphics.TestcaseStatusRenderer;
import se.kth.livetech.presentation.graphics.ColoredTextBox;
import se.kth.livetech.presentation.graphics.ImageRenderer;
import se.kth.livetech.presentation.graphics.PartitionedRowRenderer;
import se.kth.livetech.presentation.graphics.Renderable;
import se.kth.livetech.util.DebugTrace;
import se.kth.livetech.util.Frame;
import se.kth.livetech.util.Optional;

@SuppressWarnings("serial")
public class JudgeQueueTest extends JPanel {
	final int N = 20;
	final int P = 10;
	final int T = 17;
	int[] state = new int[N];
	public JudgeQueueTest() {
		this.setBackground(Color.BLUE.darker());
		this.setPreferredSize(new Dimension(600, 600));
		new TestJudge().start();
	}
	private class TestJudge extends Thread {
		public void run() {
			// TODO More realistic judge simulation...
			while (true) {
				try {
					sleep((int) (Math.random() * T));
				} catch (InterruptedException e) { }
				for (int i = 0; i < N; ++i) {
					int p = (int) (Math.random() * P * N * 100);
					if (state[i] < 0 && p == 0)
						state[i] = P + 5;
					if (state[i] < P + 5) {
						if (p < 10 && state[i] != 0)
							state[i] = -state[i];
						else if (p <= state[i] * 100)
							++state[i];
					}
					else {
						for (int j = i + 1; j < N; ++j)
							state[j - 1] = state[j];
						state[N - 1] = 0;
					}
				}
				repaint();
			}
		}
	}
	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;

		Rectangle2D rect = new Rectangle2D.Double(
				getWidth() / 10, getHeight() / 10,
				8 * getWidth() / 10, 8 * getHeight() / 10);

		Rectangle2D row = new Rectangle2D.Double();
		Dimension dim = new Dimension();
		for (int i = 0; i < N; ++i) {
			Color row1 = ICPCColors.BG_COLOR_1;
			Color row2 = ICPCColors.BG_COLOR_2;
			Renderable back;
			if (i % 2 == 0)
				back = new RowFrameRenderer(row1, row2);
			else
				back = new RowFrameRenderer(row2, row1);

			PartitionedRowRenderer<Integer> r =
				new PartitionedRowRenderer<Integer>(Optional.v(back));

			{
				String country = ICPCImages.COUNTRY_CODES[i];
				BufferedImage image = ICPCImages.getFlag(country);
				Renderable flag = new ImageRenderer("flag " + country, image);
				r.add(-3, flag, 1, true);
			}
			{
				BufferedImage image = ICPCImages.getTeamLogo(i);
				Renderable logo = new ImageRenderer("logo " + i, image);
				r.add(-2, logo, 1, true);
			}
			{
				// TODO: team name should be in a TeamSubmissionState...
				Renderable teamName = new ColoredTextBox("team" + i, ContentProvider.getTeamNameStyle());
				r.add(-1, teamName, 1, false);
			}
			for (int j = 0; j < P; ++j) {
				TestcaseStatusRenderer.Status status;
				if (j < Math.abs(state[i]))
					status = TestcaseStatusRenderer.Status.passed;
				else if (j == state[i])
					status = TestcaseStatusRenderer.Status.active;
				else if (j == -state[i])
					status = TestcaseStatusRenderer.Status.failed;
				else
					status = TestcaseStatusRenderer.Status.none;
				Renderable testcase = new TestcaseStatusRenderer(status);
				r.add(j, testcase, 1, true);
			}

			Rect.setRow(rect, i, N, row);
			Rect.setDim(row, dim);
			int x = (int) row.getX();
			int y = (int) row.getY();
			g.translate(x, y);
			r.render(g, dim);
			g.translate(-x, -y);
		}
	}
	public static void main(String[] args) {
		new Frame("JudgeQueueTest", new JudgeQueueTest());
	}
}