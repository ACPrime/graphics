package se.kth.livetech.presentation.layout;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import se.kth.livetech.presentation.animation.AnimationStack;
import se.kth.livetech.presentation.animation.Interpolated;

/**
 * An animator that takes layout scene updates and animates transitions between them,
 * itself being a layout scene update.
 */
public class LayoutSceneAnimator implements ISceneLayout {

	@SuppressWarnings("serial")
	private static class CompRect extends Rectangle2D.Double implements Comparable<CompRect> {
		public CompRect() {
		}
		public CompRect(Rectangle2D rect) {
			this.setRect(rect);
		}
		@Override
		public int compareTo(CompRect that) {
			double dy = this.getY() - that.getY();
			return dy < -1e-9 ? -1 : dy > 1e-9 ? 1 : 0;
		}
	}
	// TODO animation stack of rectangles
	private Object key;
	private Content content;
	private CompRect rect = new CompRect();
	private AnimationStack<Object, CompRect> stack;
	private Map<Object, LayoutSceneAnimator> subs = new HashMap<Object, LayoutSceneAnimator>();
	private SortedSet<Object> layers;
	
	public LayoutSceneAnimator(ISceneLayout scene) {
		this.stack = new AnimationStack<Object, CompRect>();
		this.rect.setRect(scene.getBounds());
		update(scene);
	}
	
	public void update(ISceneLayout update) {
		this.key = update.getKey();
		this.content = update.getContent();
		this.rect = new CompRect(update.getBounds());
		this.stack.setPosition(this.key, this.rect);
		Set<Object> remSet = new HashSet<Object>(this.subs.keySet());
		for (ISceneLayout sub : update.getSubs()) {
			remSet.remove(sub.getKey());
			if (subs.containsKey(sub.getKey())) {
				subs.get(sub.getKey()).update(sub);
			} else {
				subs.put(sub.getKey(), new LayoutSceneAnimator(sub));
			}
		}
		for (Object rem : remSet) {
			subs.remove(rem);
		}
		this.layers = update.getLayers();
	}
	
	public boolean advance(double advance) {
		boolean updated = this.stack.advance(advance);
		for (LayoutSceneAnimator sub : subs.values()) {
			updated |= sub.advance(advance);
		}
		return updated;
	}

	@Override
	public Object getKey() {
		return this.key;
	}

	@Override
	public Rectangle2D getBounds() {
		CompRect r = new CompRect(this.rect);
		Interpolated.Rectangle<CompRect> interp = new Interpolated.Rectangle<CompRect>(r);
		this.stack.interpolate(this.key, interp);
		return interp.getValue();
	}

	@Override
	public Content getContent() {
		return this.content;
	}

	@Override
	public List<ISceneLayout> getSubs() {
		// TODO: Change return type to Collection instead?
		return new ArrayList<ISceneLayout>(this.subs.values());
	}

	@Override
	public SortedSet<Object> getLayers() {
		return this.layers;
	}
}
