package winServ;

import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final public class SingletonCoordinator {
	private  Lock lock_rw;
	private  Condition cond;
	private static SingletonCoordinator instance = null;
	private  volatile AtomicBoolean main_blocked_sel = new AtomicBoolean(false);//true if the main thread is about to call select
	private  volatile AtomicInteger threads_waiting = new AtomicInteger(0); // > 0 if there are threads waiting after select.wakeup()
	private  volatile AtomicBoolean wakeup_called = new AtomicBoolean(true);// true if one thread has called wakeup
	
	private SingletonCoordinator() {
		this.lock_rw = new ReentrantLock();
		this.cond = this.lock_rw.newCondition();
	}
	
	public static synchronized SingletonCoordinator getCoordinator() {
		if(instance == null) {
			instance = new SingletonCoordinator();
		}
		return instance;
	}
	
	public void wake_main_and_wait(Selector sel) {
		if(this.main_blocked_sel.get()) {
			if(!this.wakeup_called.get()) {
				sel.wakeup();
				this.wakeup_called.set(true);
			}
			try {
				lock_rw.lock();
				this.threads_waiting.incrementAndGet();
				this.cond.await();
				this.threads_waiting.getAndDecrement();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				lock_rw.unlock();
			}
			
		}
	}
	
	public void set_main_blocking() {
		this.main_blocked_sel.set(true);
		this.wakeup_called.set(false);
	}
	
	public void set_main_non_blocking_wake_threads() {
		this.main_blocked_sel.set(false);
		if(this.threads_waiting.get() > 0) {
			this.lock_rw.lock();
			this.cond.signalAll();
			this.lock_rw.unlock();
		}
	}
}
