package com.paloski.time.clock;

import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class DateClockTest {

	/**
	 * Performs tests on the
	 * {@link DateClock#ofSupplier(java.util.function.Supplier, ZoneId)}
	 * methods.
	 * 
	 * @author Adam Paloski
	 *
	 */
	public static class OfSupplierTests {

		@Rule
		public ExpectedException expected = ExpectedException.none();

		@Test
		public void nullSupplierThrows() {
			expected.expect(NullPointerException.class);
			expected.expectMessage("The date supplier may not be null");
			DateClock.ofSupplier(null, ZoneId.systemDefault());
		}

		@Test
		public void nullZoneIdThrows() {
			expected.expect(NullPointerException.class);
			expected.expectMessage("The zoneid may not be null");
			DateClock.ofSupplier(() -> new Date(), null);
		}

		@Test
		public void nullDateSupplierCausesNPE() {
			// No message, we let failure in null throw on its own, we clearly
			// state it must return non-null.
			expected.expect(NullPointerException.class);
			DateClock.ofSupplier(() -> null, ZoneId.systemDefault()).instant();
		}

		/**
		 * A test that ensures that any caching is done supplier side, not on
		 * ours.
		 */
		@Test
		public void testSupplierIsQueriedEveryInstantCall() {
			AtomicInteger callCount = new AtomicInteger();
			Supplier<Date> dateSupplier = () -> {
				callCount.incrementAndGet();
				return Date.from(Instant.now());
			};
			Clock clock = DateClock.ofSupplier(dateSupplier, ZoneId.systemDefault());

			for (int x = 0; x < 10; ++x) {
				clock.instant();
			}

			assertEquals(10, callCount.get());
		}

		@Test
		public void testSupplierIsQueriedEveryMillisCall() {
			AtomicInteger callCount = new AtomicInteger();
			Supplier<Date> dateSupplier = () -> {
				callCount.incrementAndGet();
				return Date.from(Instant.now());
			};
			Clock clock = DateClock.ofSupplier(dateSupplier, ZoneId.systemDefault());

			for (int x = 0; x < 10; ++x) {
				clock.millis();
			}

			assertEquals(10, callCount.get());
		}

	}

	public static class GetDateTests {

		@Rule
		public ExpectedException expected = ExpectedException.none();

		/**
		 * A simple test date clock used in tests of this class
		 */
		private static abstract class TestDateClock extends DateClock {
			@Override
			public ZoneId getZone() {
				return ZoneId.systemDefault();
			}

			@Override
			public Clock withZone(ZoneId zone) {
				throw new UnsupportedOperationException();
			}
		}

		@Test
		public void returningNullFromGetDateThrows() {
			DateClock clock = new TestDateClock() {
				@Override
				protected Date getDate() {
					return null;
				}
			};
			expected.expect(NullPointerException.class);
			clock.instant();
		}

		/**
		 * Tests that our final methods at least don't modify the dates
		 * returned, we can't check users.
		 */
		@Test
		public void dateReturnedByGetDateIsNotModified() {
			Date original = Date.from(Instant.ofEpochMilli(34644612));
			Date originalClone = (Date) original.clone();
			// Make sure we're equal before in case someone did something weird
			// in the JVM
			assertEquals(original, originalClone);
			DateClock clock = new TestDateClock() {
				@Override
				protected Date getDate() {
					return original;
				}
			};
			clock.instant();
			clock.millis();
			assertEquals(original, originalClone);
		}

		@DataPoints
		public static Instant[] instants() {
			return new Instant[] { Instant.ofEpochMilli(0), Instant.ofEpochSecond(122231), Instant.ofEpochSecond(-12233),
					Instant.ofEpochSecond(1000000000) };
		}

		@Theory
		public void instantFromDateIsEqualToInstantGoingIn(Instant in) {
			Date original = Date.from(in);
			DateClock clock = new TestDateClock() {
				@Override
				protected Date getDate() {
					return original;
				}
			};
			assertEquals(clock.instant(), in);
			assertEquals(clock.millis(), in.toEpochMilli());
		}

	}
}
