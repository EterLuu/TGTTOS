package ink.ziip.hammer.tgttos.api.task;

import ink.ziip.hammer.tgttos.TGTTOS;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class BaseTask extends BukkitRunnable {

    protected boolean started;
    protected int period;

    public BaseTask(int period) {
        this.started = false;
        this.period = period;
    }

    public void start() {
        this.runTaskTimerAsynchronously(TGTTOS.getInstance(), 1, period);
        started = true;
    }

    public abstract void stop();
}
