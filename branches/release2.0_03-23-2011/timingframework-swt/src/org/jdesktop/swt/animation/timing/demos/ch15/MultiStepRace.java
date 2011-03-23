package org.jdesktop.swt.animation.timing.demos.ch15;

import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.jdesktop.core.animation.timing.Animator;
import org.jdesktop.core.animation.timing.Animator.RepeatBehavior;
import org.jdesktop.core.animation.timing.AnimatorBuilder;
import org.jdesktop.core.animation.timing.Interpolator;
import org.jdesktop.core.animation.timing.KeyFrames;
import org.jdesktop.core.animation.timing.KeyTimes;
import org.jdesktop.core.animation.timing.KeyValues;
import org.jdesktop.core.animation.timing.PropertySetter;
import org.jdesktop.core.animation.timing.TimingSource;
import org.jdesktop.core.animation.timing.interpolators.SplineInterpolator;
import org.jdesktop.swt.animation.timing.sources.SWTTimingSource;
import org.jdesktop.swt.animation.timing.triggers.EventTrigger;

/**
 * The full-blown demo with all of the bells and whistles. This one uses the
 * facilities shown in all of the other variations, but adds both multi-step and
 * non-linear interpolation. It does this by creating a KeyFrames object to hold
 * the times/values/splines used for each segment of the race. It also adds an
 * animation for the rotation of the car (since the car should turn as it goes
 * around the curves) and sound effects (just to go completely overboard).
 * 
 * @author Chet Haase
 * @author Tim Halloran
 */
public final class MultiStepRace {

	public static void main(String args[]) {
		final Display display = Display.getDefault();
		final Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		final TimingSource ts = new SWTTimingSource(15, TimeUnit.MILLISECONDS,
				display);
		AnimatorBuilder.setDefaultTimingSource(ts);
		ts.init();

		final MultiStepRace race = new MultiStepRace(shell,
				"SWT Multi-Step Race");

		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		race.stopSoundEffects();
		ts.dispose();
		display.dispose();
		System.exit(0);
	}

	public static final int RACE_TIME = 10000;

	private final Animator animator;
	private final SoundEffects soundEffects;

	/** Creates a new instance of BasicRace */
	public MultiStepRace(Shell shell, String appName) {
		RaceGUI basicGUI = new RaceGUI(shell, appName);

		// We're going to need a more involved PropertyRange object
		// that has all curves of the track in it, as well as
		// non-linear movement around the curves
		Point values[] = { TrackView.START_POS, TrackView.FIRST_TURN_START,
				TrackView.FIRST_TURN_END, TrackView.SECOND_TURN_START,
				TrackView.SECOND_TURN_END, TrackView.THIRD_TURN_START,
				TrackView.THIRD_TURN_END, TrackView.FOURTH_TURN_START,
				TrackView.START_POS };
		final KeyValues<Point> keyValues = KeyValues.build(values);
		// Calculate the keyTimes based on the distances that must be
		// traveled on each leg of the journey
		double totalDistance = 0;
		double segmentDistance[] = new double[values.length];
		for (int i = 0; i < (values.length - 1); ++i) {
			segmentDistance[i] = distance(values[i], values[i + 1]);
			totalDistance += segmentDistance[i];
		}
		segmentDistance[(values.length - 1)] = distance(
				values[(values.length - 1)], values[0]);
		totalDistance += segmentDistance[(values.length - 1)];
		double times[] = new double[values.length];
		double elapsedTime = 0.0f;
		times[0] = 0.0f;
		times[values.length - 1] = 1.0f;
		for (int i = 0; i < (values.length - 2); ++i) {
			times[i + 1] = elapsedTime + (segmentDistance[i] / totalDistance);
			elapsedTime = times[i + 1];
		}
		final KeyTimes keyTimes = KeyTimes.build(times);

		/*
		 * For realistic movement, we want a big acceleration on the
		 * straightaways.
		 */
		Interpolator initialSpline = new SplineInterpolator(1.00f, 0.00f, 0.2f,
				.2f);
		Interpolator straightawaySpline = new SplineInterpolator(0.50f, 0.20f,
				.50f, .80f);
		Interpolator curveSpline = new SplineInterpolator(0.50f, 0.20f, .50f,
				.80f);
		Interpolator finalSpline = new SplineInterpolator(0.50f, 0.00f, .50f,
				1.00f);
		final KeyFrames<Point> keyFrames = KeyFrames.build(keyValues, keyTimes,
				initialSpline, curveSpline, straightawaySpline, curveSpline,
				straightawaySpline, curveSpline, straightawaySpline,
				finalSpline);

		/*
		 * This PropertySetter enables the animation for the car movement all
		 * the way around the track.
		 */
		final PropertySetter modifier = new PropertySetter(basicGUI.getTrack(),
				"carPosition", keyFrames);
		animator = new AnimatorBuilder()
				.setDuration(RACE_TIME, TimeUnit.MILLISECONDS)
				.setRepeatCount(Animator.INFINITE)
				.setRepeatBehavior(RepeatBehavior.LOOP).addTarget(modifier)
				.build();

		/*
		 * Now create similar keyframes for rotation of car.
		 */
		KeyValues<Integer> rotationKeyValues = KeyValues.build(360, 315, 270,
				225, 180, 135, 90, 45, 0);
		Interpolator straightawayTurnSpline = new SplineInterpolator(1.0f,
				0.0f, 1.0f, 0.0f);
		Interpolator curveTurnSpline = new SplineInterpolator(0.0f, 0.5f, 0.5f,
				1.0f);
		KeyFrames<Integer> rotationKeyFrames = KeyFrames.build(
				rotationKeyValues, keyTimes, straightawayTurnSpline,
				curveTurnSpline, straightawayTurnSpline, curveTurnSpline,
				straightawayTurnSpline, curveTurnSpline,
				straightawayTurnSpline, curveTurnSpline);
		final PropertySetter rotationModifier = new PropertySetter(
				basicGUI.getTrack(), "carRotation", rotationKeyFrames);
		animator.addTarget(rotationModifier);

		/*
		 * Finally, add sound effects, triggered by the same animator.
		 */
		soundEffects = new SoundEffects(rotationKeyFrames);
		animator.addTarget(soundEffects);

		/*
		 * Instead of manually tracking the events, have the framework do the
		 * work by setting up a trigger.
		 */
		Button goButton = basicGUI.getControlPanel().getGoButton();
		Button pauseResumeButton = basicGUI.getControlPanel()
				.getPauseResumeButton();
		Button stopButton = basicGUI.getControlPanel().getStopButton();
		EventTrigger.addTrigger(goButton, SWT.Selection, animator);
		pauseResumeButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (animator.isPaused()) {
					animator.resume();
				} else {
					if (animator.isRunning())
						animator.pause();
				}
			}
		});
		stopButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (animator.isRunning())
					animator.stop();
				stopSoundEffects();
			}
		});
	}

	public void stopSoundEffects() {
		if (soundEffects != null)
			soundEffects.stop();
	}

	public static double distance(Point pt1, Point pt2) {
		double px = pt1.x - pt2.x;
		double py = pt1.y - pt2.y;
		return Math.sqrt(px * px + py * py);
	}
}
