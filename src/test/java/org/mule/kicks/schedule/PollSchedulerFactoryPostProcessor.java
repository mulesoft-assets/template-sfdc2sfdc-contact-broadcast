package org.mule.kicks.schedule;

import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;
import org.mule.transport.polling.schedule.FixedFrequencyScheduler;

public class PollSchedulerFactoryPostProcessor implements SchedulerFactoryPostProcessor {

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Scheduler process(Object job, final Scheduler scheduler) {
		FixedFrequencyScheduler rs = (FixedFrequencyScheduler) scheduler;
		return new SynchronousScheduler(rs.getName(), rs.getFrequency(), 0, (Runnable) job, rs.getTimeUnit());
	}
}