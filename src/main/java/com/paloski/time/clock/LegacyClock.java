package com.paloski.time.clock;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

/**
 * A LegacyClock is a specialization of a Clock that encompasses another Clock
 * and uses its output values to fuel direct conversion to legacy date/time
 * types.
 * <p>
 * This clock allows direct construction of all legacy Time data types used
 * before the introduction of the <a href=
 * "https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html">
 * Java 8 Date &amp; Time APIs</a>. By allowing direct access, this Clock allows
 * you to have one point of access to all Date/Time values for your application,
 * and reduces the amount of conversion boiler plate you will need to use when
 * converting from the new date/time APIs to the legacy ones.
 * <p>
 * So in a cross API application, which has an external Time provider that
 * provides {@link Calendar} objects as a means of measuring the current point
 * in Time, this class can be used in the following way:
 * 
 * <pre>
 * {@code 
 * 	public static final LegacyClock mLegacyClock = LegacyClock.of(CalendarClock.ofSupplier(LegacyCalendarSupplier::getCurrentDateTime, ZoneId.getSystemDefault()));
 *  
 *  public void timeMethod() {
 *  	//Later when calling a method using legacy Date objects...
 *  	LegacyDateAPICall.functionDependentOnDate(mLegacyClock.toDate());
 *  
 *  	//Later when calling a method using legacy Calendar objects
 *  	mCalendarBasedObjectToSave.setCurrentDateTime(mLegacyClock.toUTCCalendar());
 *  	mCalendarBasedObjectToSave.save();
 *  
 *  	//Later when working with current date/time APIs
 *  	LocalDate date = LocalDate.of(mLegacyClock);
 *  }
 * }
 * </pre>
 * 
 * @author Adam Paloski
 *
 */
/*
 * TODO: This class is mildly inefficent when working with Calendar and Date
 * based clocks, as it preforms a Calendar -> Instant -> long -> Calendar
 * conversion. However, this also saves us from accidentally returning a Date or
 * Calendar we are contractually obligated to not modify.
 * 
 * Consider implementing the improvement, and adding Junit tests to prevent that
 * breaking case.
 */
public final class LegacyClock extends Clock implements Serializable {

	private static final long serialVersionUID = 7781357990946687384L;

	private final Clock mWrappedClock;

	/**
	 * Creates a new LegacyClock that wraps a Clock used for its underlying time
	 * amounts.
	 * 
	 * @param wrapped
	 *            A non-null clock used for underlying time measurements.
	 */
	private LegacyClock(Clock wrapped) {
		mWrappedClock = Objects.requireNonNull(wrapped);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ZoneId getZone() {
		return mWrappedClock.getZone();
	}

	/**
	 * Obtains the TimeZone equivalent to the ZoneId returned by
	 * {@link #getZone()}.
	 * 
	 * @return A TimeZone object based upon the ZoneId of this clock.
	 * @see TimeZone#getTimeZone(ZoneId)
	 */
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone(getZone());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public LegacyClock withZone(ZoneId zone) {
		return new LegacyClock(mWrappedClock.withZone(zone));
	}

	/**
	 * Returns a copy of this clock with a different time-zone, using a TimeZone
	 * object instead of a ZoneId for the conversion.
	 * <p>
	 * A clock will typically obtain the current instant and then convert that
	 * to a date or time using a time-zone. This method returns a clock with
	 * similar properties but using a different time-zone.
	 * 
	 * @param zone
	 *            A non-null TimeZone object that is then mapped to a ZoneId for
	 *            setting the TimeZone of this Clock.
	 * @return A new, non-null Clock based upon this clock with the specified
	 *         time-zone.
	 * @see #withZone(ZoneId) to convert using a ZoneId object.
	 */
	public LegacyClock withTimeZone(TimeZone zone) {
		return withZone(zone.toZoneId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instant instant() {
		return mWrappedClock.instant();
	}

	/**
	 * Converts the current time of this Clock to a Calendar object with a
	 * {@link Calendar#getTimeZone() time-zone} of UTC and a
	 * {@link Calendar#getTimeInMillis() time} equal to the current value of
	 * {@link #millis()}.
	 * 
	 * @return A new, non-null Calendar representing the same point in time time
	 *         as this Clock with a TimeZone of UTC.
	 * @see #toZonedCalendar() for a Calendar with an equivalent time-zone to
	 *      this Clock set
	 */
	public Calendar toUTCCalendar() {
		// @formatter:off
		return new Calendar.Builder()
				.setInstant(millis())
				.setTimeZone(TimeZone.getTimeZone("UTC"))
				.build();
		// @formatter:on
	}

	/**
	 * Converts the current time of this Clock to a Calendar object with
	 * {@link Calendar#getTimeZone() time-zone} equal to the TimeZone returned
	 * by {@link #getTimeZone()} and a {@link Calendar#getTimeInMillis() time}
	 * equal to the current value of {@link #millis()}.
	 * 
	 * @return A new, non-null Calendar representing the same point in time as
	 *         this Clock, in the same time-zone as this clock.
	 * @see #toUTCCalendar() for a Calendar within the UTC time-zone
	 */
	public Calendar toZonedCalendar() {
		// @formatter:off
		return new Calendar.Builder()
				.setInstant(millis())
				.setTimeZone(getTimeZone())
				.build();
		// @formatter:on
	}

	/**
	 * Converts the current time of this Clock to a Date object with a
	 * {@link Date#getTime()} value equal to the current value of
	 * {@link #millis()}.
	 * 
	 * @return A new, non-null Date object representing the same point in time
	 *         as this Clock
	 * @see #toTimestamp() for converting to a {@link Timestamp} object instead
	 *      of a Date object
	 */
	public Date toDate() {
		return new Date(millis());
	}

	/**
	 * Converts the current time of this Clock to a Timestamp object with a
	 * {@link Timestamp#getTime()} value equal to the current value of
	 * {@link #millis()}.
	 * 
	 * @return A new, non-null Date object representing the same point in time
	 *         as this Clock
	 */
	public Timestamp toTimestamp() {
		return new Timestamp(millis());
	}

	/**
	 * Obtains a LegacyClock that returns values from the specified Clock, but
	 * also allows direct access to legacy data types.
	 * <p>
	 * The Clock returned by this method will return the exact values returned
	 * by {@code other}, however it also allows direct access to legacy
	 * date/time types.
	 * <p>
	 * The returned implementation is immutable, thread-safe and
	 * {@code Serializable} providing that the base clock is.
	 * 
	 * @param otherClock
	 *            A non-null Clock that will be wrapped by this Clock and
	 *            provide the underlying time amounts used by it
	 * 
	 * @return A new Clock that wraps {@code otherClock} and allows access to
	 *         legacy time values based upon it.
	 */
	public static LegacyClock of(Clock otherClock) {
		return new LegacyClock(otherClock);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + mWrappedClock.hashCode();
		return result;
	}

	/**
	 * Computes if this LegacyClock is equivalent of {@code obj} by checking if
	 * it is also a LegacyClock, then computing if the underlying clocks are
	 * equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LegacyClock) {
			return mWrappedClock.equals(((LegacyClock) obj).mWrappedClock);
		}
		return false;
	}

}
