package org.jdesktop.swing.animation.timing.demos.ch14;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.jdesktop.core.animation.timing.Animator;
import org.jdesktop.core.animation.timing.Animator.Direction;
import org.jdesktop.core.animation.timing.AnimatorBuilder;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.core.animation.timing.TimingTargetAdapter;
import org.jdesktop.swing.animation.timing.sources.SwingTimerTimingSource;

/**
 * The simplest version of the race track animation. It sets up an
 * {@link Animator} to move the car from one position to another, linearly, over
 * a given time period.
 * <p>
 * This demo is discussed in Chapter 14 on pages 357&ndash;359 of <i>Filthy Rich
 * Clients</i> (Haase and Guy, Addison-Wesley, 2008).
 * 
 * @author Chet Haase
 */
public class BasicRace extends TimingTargetAdapter {

	public static void main(String args[]) {
		System.setProperty("swing.defaultlaf",
				"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

		TimingSource ts = new SwingTimerTimingSource(10, TimeUnit.MILLISECONDS);
		AnimatorBuilder.setDefaultTimingSource(ts);
		ts.init();
		Runnable doCreateAndShowGUI = new Runnable() {
			public void run() {
				new BasicRace("BasicRace");
			}
		};
		SwingUtilities.invokeLater(doCreateAndShowGUI);
	}

	public static final int RACE_TIME = 2000;
	Point start = TrackView.START_POS;
	Point end = TrackView.FIRST_TURN_START;
	Point current = new Point();
	protected Animator animator;
	TrackView track;
	RaceControlPanel controlPanel;

	/** Creates a new instance of BasicRace */
	public BasicRace(String appName) {
		final RaceGUI basicGUI = new RaceGUI(appName);
		controlPanel = basicGUI.getControlPanel();
		controlPanel.getGoButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				animator.stop();
				animator.start();
				basicGUI.getTrack().setCarRotation(0);
			}
		});
		controlPanel.getReverseButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (animator.isPaused()) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					return;
				}

				if (animator.isRunning()) {
					animator.reverseNow();
					basicGUI.getTrack().reverseCarRotation();
				} else {
					animator.startReverse();
					basicGUI.getTrack().setCarRotation(180);
				}
			}
		});
		controlPanel.getPauseResumeButton().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (animator.isPaused()) {
							animator.resume();
						} else {
							if (animator.isRunning())
								animator.pause();
						}
					}
				});
		controlPanel.getStopButton().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				animator.stop();
			}
		});
		track = basicGUI.getTrack();
		animator = getAnimator();
	}

	/**
	 * So that subclasses can customize.
	 * 
	 * @return an animation.
	 */
	protected Animator getAnimator() {
		return new AnimatorBuilder()
				.setDuration(RACE_TIME, TimeUnit.MILLISECONDS).addTarget(this)
				.build();
	}

	/**
	 * Calculate and set the current car position based on the animation
	 * fraction.
	 */
	@Override
	public void timingEvent(double fraction, Direction direction,
			Animator source) {
		// Simple linear interpolation to find current position
		current.x = (int) (start.x + (end.x - start.x) * fraction);
		current.y = (int) (start.y + (end.y - start.y) * fraction);

		// set the new position; this will force a repaint in TrackView
		// and will display the car in the new position
		track.setCarPosition(current);
	}
}