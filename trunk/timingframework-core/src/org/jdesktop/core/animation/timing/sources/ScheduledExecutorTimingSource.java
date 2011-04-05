package org.jdesktop.core.animation.timing.sources;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jdesktop.core.animation.timing.TimingSource;

/**
 * An implementation of {@link TimingSource} using a
 * {@link ScheduledExecutorService} as returned from
 * {@link Executors#newSingleThreadScheduledExecutor()}.
 * <p>
 * A typical use, where {@code tl} is a {@code TickListener} object, would be
 * 
 * <pre>
 * TimingSource ts = new ScheduledExecutorTimingSource(15, TimeUnit.MILLISECONDS);
 * ts.init(); // starts the timer
 * 
 * ts.addTickListener(tl); // tl gets tick notifications
 * 
 * ts.removeTickListener(tl); // tl stops getting notifications
 * 
 * ts.dispose(); // done using ts
 * </pre>
 * 
 * @author Tim Halloran
 */
public final class ScheduledExecutorTimingSource extends TimingSource {

  private final ScheduledExecutorService f_executor;
  private final long f_period;
  private final TimeUnit f_periodTimeUnit;

  /**
   * Constructs a new instance. The {@link #init()} must be called on the new
   * instance to start the timer. The {@link #dispose()} method should be called
   * to stop the timer.
   * 
   * @param period
   *          the period of time between "tick" events.
   * @param unit
   *          the time unit of period parameter.
   */
  public ScheduledExecutorTimingSource(long period, TimeUnit unit) {
    f_period = period;
    f_periodTimeUnit = unit;
    f_executor = Executors.newSingleThreadScheduledExecutor();
  }

  @Override
  public void init() {
    f_executor.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        getNotifyTickListenersTask().run();
      }
    }, 0, f_period, f_periodTimeUnit);
  }

  @Override
  public void dispose() {
    f_executor.shutdown();
  }

  @Override
  protected void runTaskInThreadContext(Runnable task) {
    f_executor.submit(task);
  }
}
