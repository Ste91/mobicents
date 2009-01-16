package javax.slee.facilities;

import javax.slee.Address;
import javax.slee.ActivityContextInterface;
import javax.slee.TransactionRolledbackLocalException;
import javax.slee.TransactionRequiredLocalException;

/**
 * The Timer Facility allows an SBB entity to set and cancel timers.  Timers
 * are identified by a serializable {@link TimerID} object.  When a new
 * timer is set, the Timer Facility generates a {@link TimerID} that
 * identifies the timer.  The timer can be cancelled at a later time by passing
 * this {@link TimerID} to the {@link #cancelTimer} method.
 * <p>
 * This facility is transactional in nature.  Timers are only set or cancelled
 * if the transaction in which their setup or cancellation took place commits
 * successfully.  A timer may be set and cancelled in the same transaction, in
 * which case the Timer Facility can consider it never set.
 * <p>
 * An SBB obtains access to a <code>TimerFacility</code> object via its JNDI
 * environment.  The Timer Facility is bound into JNDI using the name specified
 * by {@link #JNDI_NAME}.
 * <p>
 *<b>Timer Resolution</b><br>
 * Although the unit for the period of a periodic timer is specified in
 * milliseconds, a Timer Facility implementation may be limited by the
 * available clock resolution and/or scheduler implementation of the
 * underlying platform. The approximate timer resolution of the Timer Facility
 * can be obtained via {@link #getResolution}, allowing SBB entities to create
 * timers that take into account the timer resolution of the Timer Facility.
 *
 * @see TimerOptions
 * @see TimerEvent
 */
public interface TimerFacility {
    /**
     * Constant declaring the JNDI name where a <code>TimerFacility</code> object
     * is bound into an SBB's JNDI environment.
     * <p>
     * The value of this constant is "java:comp/env/slee/facilities/timer".
     * @since SLEE 1.1
     */
    public static final String JNDI_NAME = "java:comp/env/slee/facilities/timer";

    /**
     * Set a non-periodic timer.  A single timer event is generated when the
     * timer expires, after which the timer ceases to exist.  The timer event
     * is delivered on the specified Activity Context.
     *<p>
     * If the expiry time is in the past, the timer event is fired immediately
     * after the transaction commits (subject to the options specified for the
     * timer).
     *<p>
     * This method is a required transactional method.
     * @param aci the Activity Context on which the timer event should be
     *  fired.
     * @param address the optional default address on which the timer event
     *  should be fired.
     * @param expireTime the time (in ms since January 1, 1970 UTC) that the
     *  timer event should be fired.
     * @param options the desired behavior of the timer.
     * @return the timer ID of the timer that was set.
     * @throws NullPointerException if <code>aci</code> or
     *  <code>options</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>expireTime</code> is less
     *  than zero.
     * @throws TransactionRolledbackLocalException if this method was invoked without
     *        a valid transaction context and the transaction started by this method
     *        failed to commit.
     * @throws FacilityException if the timer could not be set due to a
     *  system-level failure.
     */
    public TimerID setTimer(ActivityContextInterface aci, Address address, long expireTime, TimerOptions options)
        throws NullPointerException, IllegalArgumentException,
               TransactionRolledbackLocalException, FacilityException;

    /**
     * Set a periodic timer. When the start time of a periodic timer has been
     * reached, a timer event is generated and the timer continues, generating
     * further timer events at the rate specified by the timer's period.  A
     * periodic timer may have either a finite or infinite number of
     * repetitions.  Timer events are delivered on the specified Activity
     * Context.
     *<p>
     * If the start time is in the past, the initial timer event is fired 
     * immediately after the transaction commits (subject to the options
     * specified for the timer).  Further timer events may be generated
     * (subject to the options specified for the timer) while the timer
     * catches up with the current system time.
     *<p>
     * This method is a required transactional method.
     * @param aci the Activity Context on which timer events should be fired.
     * @param address the optional default address on which timer events
     *  should be fired.
     * @param startTime the time (in ms since January 1, 1970 UTC) at which
     *  the periodic timer should first fire.
     * @param period the period (in milliseconds) between timer events.
     * @param numRepetitions the maximum number of timer events that should be
     *  generated by the timer.  If this argument is <code>0</code> an
     *  infinitely repeating timer is set.
     * @param options the desired behavior of the timer.
     * @return the timer ID of the timer that was set.
     * @throws NullPointerException if <code>aci</code> or
     *  <code>options</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>startTime</code> or
     *  <code>numRepetitions</code> is less than zero, <code>period</code> is
     *  not greater than zero, or the timeout specified in <code>options</code>
     *  is greater than <code>period</code>.
     * @throws TransactionRolledbackLocalException if this method was invoked without
     *        a valid transaction context and the transaction started by this method
     *        failed to commit.
     * @throws FacilityException if the timer could not be set due to a
     *  system-level failure.
     */
    public TimerID setTimer(ActivityContextInterface aci, Address address, long startTime, long period, int numRepetitions, TimerOptions options)
        throws NullPointerException, IllegalArgumentException,
               TransactionRolledbackLocalException, FacilityException;

    /**
     * Cancel a previously set timer.
     * <p>
     * This method has no effect if the timer has already been cancelled.
     * <p>
     * This method is a required transactional method.
     * @param timerID the timer ID of the timer to cancel.
     * @throws NullPointerException if <code>timerID</code> is
     *  <code>null</code>.
     * @throws TransactionRolledbackLocalException if this method was invoked without
     *        a valid transaction context and the transaction started by this method
     *        failed to commit.
     * @throws FacilityException if the timer could not be cancelled due to a
     *        system-level failure.
     */
    public void cancelTimer(TimerID timerID)
        throws NullPointerException, TransactionRolledbackLocalException, FacilityException;

    /**
     * Get a reference to the Activity Context on which a timer was set.  It is
     * possible to obtain a reference to the Activity Context for timers which
     * have expired, however it is not possible to obtain an Activity Context
     * for activities that have already ended in the SLEE.
     * <p>
     * This method is a mandatory transactional method.
     * @param timerID the timer ID of the timer.
     * @return an <code>ActivityContextInterface</code> object that encapsulates
     *        the Activity Context on which the timer was set.  If the underlying
     *        activity has already ended, this method returns <code>null</code>.
     * @throws NullPointerException if <code>timerID</code> is <code>null</code>.
     * @throws TransactionRequiredLocalException if this method is invoked without a
     *        valid transaction context.
     * @throws FacilityException if the Activity Context could not be obtained due
     *        to a system-level failure.
     * @since SLEE 1.1
     */
    public ActivityContextInterface getActivityContextInterface(TimerID timerID)
        throws NullPointerException, TransactionRequiredLocalException, FacilityException;

    /**
     * Get the approximate timer resolution of the Timer Facility.
     * <p>
     * This method is a non-transactional method.
     * @return the approximate timer resolution (in milliseconds).
     * @throws FacilityException if the timer resolution could not be obtained
     *  due to a system-level failure.
     */
    public long getResolution()
        throws FacilityException;

    /**
     * Get the default timeout period of the Timer Facility.
     * <p>
     * This method is a non-transactional method.
     * @return the default timeout period (in milliseconds).
     * @throws FacilityException if the default timeout period could not be
     *  obtained due to a system-level failure.
     */
    public long getDefaultTimeout()
        throws FacilityException;

}
